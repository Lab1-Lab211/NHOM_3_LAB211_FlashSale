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
import model.enums.CustomerTier;
import model.enums.LockMechanism;
import model.enums.OrderStatus;
import model.enums.SaleStatus;
import repository.CustomerRepository;
import repository.FlashSaleEventRepository;
import repository.FlashSaleItemRepository;
import repository.OrderDetailRepository;
import repository.OrderRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

    public OrderService(OrderRepository orderRepository,
                        OrderDetailRepository orderDetailRepository,
                        FlashSaleItemRepository flashSaleItemRepository,
                        FlashSaleEventRepository flashSaleEventRepository) {
        this(orderRepository, orderDetailRepository, flashSaleItemRepository, flashSaleEventRepository, null);
    }

    public OrderService(OrderRepository orderRepository,
                        OrderDetailRepository orderDetailRepository,
                        FlashSaleItemRepository flashSaleItemRepository,
                        FlashSaleEventRepository flashSaleEventRepository,
                        CustomerRepository customerRepository) {
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.flashSaleItemRepository = flashSaleItemRepository;
        this.flashSaleEventRepository = flashSaleEventRepository;
        this.customerRepository = customerRepository;
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
                OrderStatus.DA_XAC_NHAN,
                totalAmount);
        OrderDetail detail = new OrderDetail(
                detailId,
                orderId,
                flashItemId,
                quantity,
                updatedItem.getFlashPrice());

        orderRepository.save(order);
        orderDetailRepository.save(detail);
        CustomerTier tierAfterOrder = updateTierAfterSuccessfulOrder(customerToUpdate, customerId);

        return new BookingResult(order, detail, updatedItem, "Dat hang thanh cong bang " + mechanism.name(),
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
            throw new IllegalArgumentException("Moi lan chi duoc dat toi da "
                    + PURCHASE_LIMIT_PER_ITEM + " san pham");
        }
    }

    private int countBoughtQuantity(String customerId, String eventId, String flashItemId) {
        int total = 0;
        List<Order> orders = orderRepository.findByCustomerAndEvent(customerId, eventId);
        for (Order order : orders) {
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
            if (order.getStatus() == OrderStatus.DA_XAC_NHAN) {
                total += order.getTotalAmount();
            }
        }
        return total;
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
