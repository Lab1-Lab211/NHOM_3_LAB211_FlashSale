package service;

import exception.EntityNotFoundException;
import exception.EventNotActiveException;
import exception.ExceedPurchaseLimitException;
import exception.OptimisticLockException;
import exception.OutOfStockException;
import model.Customer;
import model.FlashSaleEvent;
import model.FlashSaleItem;
import model.Order;
import model.OrderDetail;
import model.Product;
import model.Seller;

import model.enums.CustomerTier;
import model.enums.LockMechanism;
import model.enums.OrderStatus;
import model.enums.SaleStatus;
import repository.CustomerRepository;
import repository.FlashSaleEventRepository;
import repository.FlashSaleItemRepository;
import repository.OrderDetailRepository;
import repository.OrderRepository;
import repository.ProductRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;

public class OrderService {
    private static final int PURCHASE_LIMIT_PER_ITEM = 2;
    private static final double PREMIUM_MIN_SPENT = 500000.0;
    private static final double VIP_MIN_SPENT = 1000000.0;
    private static final double PREMIUM_DISCOUNT_PERCENT = 5.0;
    private static final double VIP_DISCOUNT_PERCENT = 10.0;
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final FlashSaleItemRepository flashSaleItemRepository;
    private final FlashSaleEventRepository flashSaleEventRepository;
    private final CustomerRepository customerRepository;
    private ProductRepository productRepository;


    public OrderService(OrderRepository orderRepository,
                        OrderDetailRepository orderDetailRepository,
                        FlashSaleItemRepository flashSaleItemRepository,
                        FlashSaleEventRepository flashSaleEventRepository) {
        this(orderRepository, orderDetailRepository, flashSaleItemRepository,
                flashSaleEventRepository, null, null);
    }

    public OrderService(OrderRepository orderRepository,
                        OrderDetailRepository orderDetailRepository,
                        FlashSaleItemRepository flashSaleItemRepository,
                        FlashSaleEventRepository flashSaleEventRepository,
                        CustomerRepository customerRepository) {
        this(orderRepository, orderDetailRepository, flashSaleItemRepository,
                flashSaleEventRepository, customerRepository, null);
    }

    public OrderService(OrderRepository orderRepository,
                        OrderDetailRepository orderDetailRepository,
                        FlashSaleItemRepository flashSaleItemRepository,
                        FlashSaleEventRepository flashSaleEventRepository,
                        CustomerRepository customerRepository,
                        ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.flashSaleItemRepository = flashSaleItemRepository;
        this.flashSaleEventRepository = flashSaleEventRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    public void setProductRepository(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public BookingResult placeOrderNoLock(Customer customer, String flashItemId, int quantity)
            throws EntityNotFoundException, EventNotActiveException,
            ExceedPurchaseLimitException, OutOfStockException, OptimisticLockException {
        return placeOrder(customer, flashItemId, quantity, LockMechanism.NO_LOCK);
    }

    public BookingResult placeOrder(Customer customer, String flashItemId, int quantity, LockMechanism mechanism)
            throws EntityNotFoundException, EventNotActiveException,
            ExceedPurchaseLimitException, OutOfStockException, OptimisticLockException {
        if (customer == null) {
            throw new IllegalStateException("Vui long login truoc khi dat hang");
        }
        return placeOrder(customer.getCustomerId(), customer.getTier(), customer, flashItemId, quantity, mechanism);
    }

    public BookingResult placeOrderNoLock(String customerId, String flashItemId, int quantity)
            throws EntityNotFoundException, EventNotActiveException,
            ExceedPurchaseLimitException, OutOfStockException, OptimisticLockException {
        return placeOrder(customerId, flashItemId, quantity, LockMechanism.NO_LOCK);
    }

    public BookingResult placeOrder(String customerId, String flashItemId, int quantity, LockMechanism mechanism)
            throws EntityNotFoundException, EventNotActiveException,
            ExceedPurchaseLimitException, OutOfStockException, OptimisticLockException {
        return placeOrder(customerId, CustomerTier.REGULAR, null, flashItemId, quantity, mechanism);
    }

    public List<Order> getOrdersForCustomer(Customer customer) {
        if (customer == null) throw new IllegalStateException("Vui long login de xem don hang");
        return orderRepository.findByCustomer(customer.getCustomerId());
    }



    /**
     * Dat hang binh thuong (khong phai flash sale) bang productId.
     * Su dung gia goc cua san pham, tru ton kho trong products.csv.
     */
    public BookingResult placeNormalOrder(Customer customer, String productId, int quantity)
            throws EntityNotFoundException, OutOfStockException {
        if (customer == null) throw new IllegalStateException("Vui long login truoc khi dat hang");
        if (productRepository == null) throw new IllegalStateException("ProductRepository chua duoc khoi tao");
        validateNormalQuantity(quantity);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product", productId));

        if (product.getStock() < quantity) {
            throw new OutOfStockException(productId, quantity, product.getStock());
        }

        // Tru ton kho
        product.setStock(product.getStock() - quantity);
        productRepository.update(product);

        String orderId = nextOrderId();
        String detailId = nextDetailId();
        CustomerTier tier = customer.getTier();
        double subtotalAmount = quantity * product.getOriginalPrice();
        double discountPercent = discountPercentForTier(tier);
        double discountAmount = subtotalAmount * discountPercent / 100.0;
        double totalAmount = subtotalAmount - discountAmount;

        // Dat hang binh thuong dung eventId = "NORMAL"
        Order order = new Order(
                orderId,
                customer.getCustomerId(),
                "NORMAL",
                LocalDateTime.now().format(DATE_TIME_FORMATTER),
                OrderStatus.CHO_XU_LY,
                totalAmount);

        // Dung productId truc tiep lam flashItemId cho order detail
        OrderDetail detail = new OrderDetail(
                detailId,
                orderId,
                productId,
                quantity,
                product.getOriginalPrice());

        orderRepository.save(order);
        orderDetailRepository.save(detail);
        CustomerTier tierAfter = tier;

        // Tao mot BookingResult gia lap (khong co FlashSaleItem/Event)
        return new BookingResult(order, detail, null, "Dat hang thanh cong, cho shop xac nhan",
                tier, tierAfter, subtotalAmount, discountPercent, discountAmount);
    }

    public BookingResult placeRegularProductOrder(Customer customer, String productId, int quantity)
            throws EntityNotFoundException, OutOfStockException {
        if (customer == null) {
            throw new IllegalStateException("Vui long login truoc khi dat hang");
        }
        if (productRepository == null) {
            throw new IllegalStateException("Chuc nang dat san pham thuong chua duoc khoi tao");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("So luong phai lon hon 0");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product", productId));
        CustomerTier tierBeforeOrder = customer.getTier();
        double subtotalAmount = quantity * product.getOriginalPrice();
        double discountPercent = discountPercentForTier(tierBeforeOrder);
        double discountAmount = subtotalAmount * discountPercent / 100.0;
        double totalAmount = subtotalAmount - discountAmount;

        productRepository.sellRegularProduct(productId, quantity);
        String orderId = nextOrderId();
        Order order = new Order(orderId, customer.getCustomerId(), "REGULAR",
                LocalDateTime.now().format(DATE_TIME_FORMATTER),
                OrderStatus.CHO_XU_LY, totalAmount);
        OrderDetail detail = new OrderDetail(nextDetailId(), orderId, productId,
                quantity, product.getOriginalPrice());
        orderRepository.save(order);
        orderDetailRepository.save(detail);
        CustomerTier tierAfterOrder = tierBeforeOrder;

        return new BookingResult(order, detail, null, "Dat hang thanh cong, cho shop xac nhan",
                tierBeforeOrder, tierAfterOrder, subtotalAmount, discountPercent, discountAmount);
    }

    public List<Order> getOrdersForSeller(Seller seller) {
        requireSeller(seller);
        Set<String> ownedItemReferences = new HashSet<>(seller.getProductIds());
        for (FlashSaleItem item : flashSaleItemRepository.findAll()) {
            if (seller.ownsProduct(item.getProductId())) {
                ownedItemReferences.add(item.getFlashItemId());
            }
        }
        Set<String> ownedOrderIds = new HashSet<>();
        for (OrderDetail detail : orderDetailRepository.findAll()) {
            if (ownedItemReferences.contains(detail.getFlashItemId())) {
                ownedOrderIds.add(detail.getOrderId());
            }
        }
        return orderRepository.findBy(order -> seller.ownsEvent(order.getEventId())
                || ownedOrderIds.contains(order.getOrderId()));
    }

    public Order getOrderForSeller(Seller seller, String orderId) throws EntityNotFoundException {
        requireSeller(seller);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order", orderId));
        if (!sellerOwnsOrder(seller, order)) {
            throw new IllegalArgumentException("Don hang khong thuoc san pham cua ban");
        }
        return order;
    }

    public List<OrderDetail> getOrderDetailsForSeller(Seller seller, String orderId)
            throws EntityNotFoundException {
        getOrderForSeller(seller, orderId);
        return orderDetailRepository.findByOrder(orderId);
    }

    public Optional<FlashSaleItem> findFlashSaleItem(String flashItemId) {
        return flashSaleItemRepository.findById(flashItemId);
    }

    public synchronized Order updateOrderStatusForSeller(Seller seller, String orderId, OrderStatus newStatus)
            throws EntityNotFoundException {
        Order order = getOrderForSeller(seller, orderId);
        if (newStatus == null) {
            throw new IllegalArgumentException("Trang thai moi khong hop le");
        }
        if (newStatus == OrderStatus.TU_CHOI) {
            if (order.getStatus() != OrderStatus.CHO_XU_LY) {
                throw new IllegalArgumentException("Chi duoc tu choi don dang cho shop xac nhan");
            }
            restoreOrderInventory(order);
            order.setStatus(OrderStatus.TU_CHOI);
            orderRepository.update(order);
            return order;
        }
        if (newStatus == OrderStatus.GIAO_THAT_BAI) {
            if (order.getStatus() != OrderStatus.DANG_GIAO) {
                throw new IllegalArgumentException("Chi duoc bao giao that bai khi don dang giao hang");
            }
            restoreOrderInventory(order);
            order.setStatus(OrderStatus.GIAO_THAT_BAI);
            orderRepository.update(order);
            return order;
        }

        OrderStatus expected = nextSellerStatus(order.getStatus());
        if (expected == null) {
            throw new IllegalArgumentException("Don hang o trang thai "
                    + order.getStatus().getMoTa() + " khong the cap nhat tiep");
        }
        if (newStatus != expected) {
            throw new IllegalArgumentException("Chuyen trang thai khong hop le. Trang thai tiep theo phai la: "
                    + expected.getMoTa());
        }
        order.setStatus(newStatus);
        orderRepository.update(order);
        return order;

    }

    public synchronized Order confirmOrderReceived(Customer customer, String orderId)
            throws EntityNotFoundException {
        if (customer == null) {
            throw new IllegalStateException("Vui long login de xac nhan da nhan hang");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order", orderId));
        if (!order.getCustomerId().equalsIgnoreCase(customer.getCustomerId())) {
            throw new IllegalArgumentException("Ban khong co quyen xac nhan don hang nay");
        }
        if (order.getStatus() != OrderStatus.DANG_GIAO) {
            throw new IllegalArgumentException("Chi duoc xac nhan da nhan khi don dang giao hang");
        }

        order.setStatus(OrderStatus.HOAN_THANH);
        orderRepository.update(order);
        updateTierAfterSuccessfulOrder(customer, customer.getCustomerId());
        return order;
    }

    public synchronized Order cancelOrder(Customer customer, String orderId)
            throws EntityNotFoundException {
        if (customer == null) throw new IllegalStateException("Vui long login de huy don hang");
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order", orderId));
        if (!order.getCustomerId().equalsIgnoreCase(customer.getCustomerId())) {
            throw new IllegalArgumentException("Ban khong co quyen huy don hang nay");
        }
        if (order.getStatus() != OrderStatus.CHO_XU_LY) {
            throw new IllegalArgumentException("Chi duoc huy don dang cho shop xac nhan");
        }

        restoreOrderInventory(order);
        order.setStatus(OrderStatus.DA_HUY);
        orderRepository.update(order);
        return order;
    }

    private void restoreOrderInventory(Order order) throws EntityNotFoundException {
        for (OrderDetail detail : orderDetailRepository.findByOrder(order.getOrderId())) {
            String itemReference = detail.getFlashItemId();
            if (itemReference != null && itemReference.startsWith("PRD-")) {
                if (productRepository == null) {
                    throw new IllegalStateException("Khong the hoan kho san pham thuong");
                }
                productRepository.restoreRegularProductStock(itemReference, detail.getQuantity());
            } else {
                flashSaleItemRepository.restoreSoldQuantity(itemReference, detail.getQuantity());
            }
        }
    }

    private BookingResult placeOrder(String customerId, CustomerTier tierBeforeOrder,
                                     Customer customerToUpdate, String flashItemId, int quantity,
                                     LockMechanism mechanism)
            throws EntityNotFoundException, EventNotActiveException,
            ExceedPurchaseLimitException, OutOfStockException, OptimisticLockException {
        if (mechanism == null) {
            mechanism = LockMechanism.NO_LOCK;
        }
        validateQuantity(quantity);

        FlashSaleItem item = flashSaleItemRepository.findById(flashItemId)
                .orElseThrow(() -> new EntityNotFoundException("FlashSaleItem", flashItemId));

        FlashSaleEvent event = flashSaleEventRepository.findById(item.getEventId())
                .orElseThrow(() -> new EntityNotFoundException("FlashSaleEvent", item.getEventId()));

        if (event.getStatus() != SaleStatus.DANG_DIEN_RA) {
            throw new EventNotActiveException(event.getEventId(), event.getStatus().name());
        }

        int boughtQuantity = countBoughtQuantity(customerId, event.getEventId(), flashItemId);
        if (boughtQuantity + quantity > PURCHASE_LIMIT_PER_ITEM) {
            throw new ExceedPurchaseLimitException(
                    customerId, flashItemId, boughtQuantity, PURCHASE_LIMIT_PER_ITEM);
        }

        sellStock(flashItemId, quantity, mechanism);
        FlashSaleItem updatedItem = flashSaleItemRepository.findById(flashItemId)
                .orElseThrow(() -> new EntityNotFoundException("FlashSaleItem", flashItemId));

        String orderId = nextOrderId();
        String detailId = nextDetailId();
        double subtotalAmount = quantity * updatedItem.getFlashPrice();
        double discountPercent = discountPercentForTier(tierBeforeOrder);
        double discountAmount = subtotalAmount * discountPercent / 100.0;
        double totalAmount = subtotalAmount - discountAmount;

        Order order = new Order(
                orderId,
                customerId,
                event.getEventId(),
                LocalDateTime.now().format(DATE_TIME_FORMATTER),
                OrderStatus.CHO_XU_LY,
                totalAmount);
        OrderDetail detail = new OrderDetail(
                detailId,
                orderId,
                flashItemId,
                quantity,
                updatedItem.getFlashPrice());

        orderRepository.save(order);
        orderDetailRepository.save(detail);
        CustomerTier tierAfterOrder = tierBeforeOrder;

        return new BookingResult(order, detail, updatedItem,
                "Dat hang thanh cong bang " + mechanism.name() + ", cho shop xac nhan",
                tierBeforeOrder, tierAfterOrder, subtotalAmount, discountPercent, discountAmount);
    }

    private void sellStock(String flashItemId, int quantity, LockMechanism mechanism)
            throws OutOfStockException, EntityNotFoundException, OptimisticLockException {
        switch (mechanism) {
            case FILE_LOCK:
                flashSaleItemRepository.sellWithFileLock(flashItemId, quantity);
                break;
            case SYNCHRONIZED:
                flashSaleItemRepository.sellWithSynchronized(flashItemId, quantity);
                break;
            case OPTIMISTIC:
                flashSaleItemRepository.sellWithOptimisticLock(flashItemId, quantity);
                break;
            case NO_LOCK:
            default:
                flashSaleItemRepository.sellNoLock(flashItemId, quantity);
                break;
        }
    }

    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("So luong phai lon hon 0");
        }
        if (quantity > PURCHASE_LIMIT_PER_ITEM) {
            throw new IllegalArgumentException("Flash Sale chi duoc dat toi da "
                    + PURCHASE_LIMIT_PER_ITEM + " san pham moi lan");
        }
    }

    /** Dat hang binh thuong: khong gioi han so luong, chi can > 0 va du ton kho. */
    private void validateNormalQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("So luong phai lon hon 0");
        }
    }

    private int countBoughtQuantity(String customerId, String eventId, String flashItemId) {
        int total = 0;
        List<Order> orders = orderRepository.findByCustomerAndEvent(customerId, eventId);
        for (Order order : orders) {
            if (order.getStatus() == OrderStatus.DA_HUY
                    || order.getStatus() == OrderStatus.TU_CHOI
                    || order.getStatus() == OrderStatus.GIAO_THAT_BAI) {
                continue;
            }
            total += orderDetailRepository.soLuongDaMuaTrongDon(order.getOrderId(), flashItemId);
        }
        return total;
    }

    private double discountPercentForTier(CustomerTier tier) {
        if (tier == CustomerTier.VIP) {
            return VIP_DISCOUNT_PERCENT;
        }
        if (tier == CustomerTier.PREMIUM) {
            return PREMIUM_DISCOUNT_PERCENT;
        }
        return 0.0;
    }

    private CustomerTier updateTierAfterSuccessfulOrder(Customer customer, String customerId) {
        CustomerTier currentTier = customer != null ? customer.getTier() : CustomerTier.REGULAR;
        CustomerTier calculatedTier = calculateTierByTotalSpent(totalConfirmedSpent(customerId));
        if (calculatedTier.getDoUuTien() < currentTier.getDoUuTien()) {
            if (customer != null) {
                customer.setTier(calculatedTier);
                if (customerRepository != null) {
                    customerRepository.update(customer);
                }
            }
            return calculatedTier;
        }
        return currentTier;
    }

    private double totalConfirmedSpent(String customerId) {
        double total = 0.0;
        for (Order order : orderRepository.findByCustomer(customerId)) {
            if (isSuccessfulOrder(order.getStatus())) {
                total += order.getTotalAmount();
            }
        }
        return total;
    }

    private void requireSeller(Seller seller) {
        if (seller == null) throw new IllegalStateException("Vui long dang nhap nguoi ban");
    }

    private boolean sellerOwnsOrder(Seller seller, Order order) {
        if (seller.ownsEvent(order.getEventId())) return true;
        for (OrderDetail detail : orderDetailRepository.findByOrder(order.getOrderId())) {
            String reference = detail.getFlashItemId();
            if (reference != null && reference.startsWith("PRD-")
                    && seller.ownsProduct(reference)) {
                return true;
            }
            Optional<FlashSaleItem> item = flashSaleItemRepository.findById(reference);
            if (item.isPresent() && seller.ownsProduct(item.get().getProductId())) {
                return true;
            }
        }
        return false;
    }

    private OrderStatus nextSellerStatus(OrderStatus current) {
        switch (current) {
            case CHO_XU_LY: return OrderStatus.DA_XAC_NHAN;
            case DA_XAC_NHAN: return OrderStatus.DANG_CHUAN_BI;
            case DANG_CHUAN_BI: return OrderStatus.DANG_GIAO;
            default: return null;
        }
    }

    private boolean isSuccessfulOrder(OrderStatus status) {
<<<<<<< HEAD
        return status == OrderStatus.CHO_XU_LY
                || status == OrderStatus.DA_XAC_NHAN
                || status == OrderStatus.DANG_CHUAN_BI
                || status == OrderStatus.DANG_GIAO
                || status == OrderStatus.HOAN_THANH;
=======
        return status == OrderStatus.HOAN_THANH;
>>>>>>> a5a83dcd015cbb535c01ed6490fef921c8fc0561
    }

    private CustomerTier calculateTierByTotalSpent(double totalSpent) {
        if (totalSpent >= VIP_MIN_SPENT) {
            return CustomerTier.VIP;
        }
        if (totalSpent >= PREMIUM_MIN_SPENT) {
            return CustomerTier.PREMIUM;
        }
        return CustomerTier.REGULAR;
    }

    private String nextOrderId() {
        int max = 0;
        for (Order order : orderRepository.findAll()) {
            max = Math.max(max, extractNumber(order.getOrderId(), "ORD-"));
        }
        return String.format("ORD-%05d", max + 1);
    }

    private String nextDetailId() {
        int max = 0;
        for (OrderDetail detail : orderDetailRepository.findAll()) {
            max = Math.max(max, extractNumber(detail.getDetailId(), "DTL-"));
        }
        return String.format("DTL-%05d", max + 1);
    }

    private int extractNumber(String id, String prefix) {
        if (id == null || !id.startsWith(prefix)) {
            return 0;
        }
        try {
            return Integer.parseInt(id.substring(prefix.length()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
