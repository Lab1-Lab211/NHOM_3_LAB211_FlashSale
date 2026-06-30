package service;

import exception.EntityNotFoundException;
import exception.EventNotActiveException;
import exception.ExceedPurchaseLimitException;
import exception.OutOfStockException;
import model.FlashSaleEvent;
import model.FlashSaleItem;
import model.Order;
import model.OrderDetail;
import model.enums.OrderStatus;
import model.enums.SaleStatus;
import repository.FlashSaleEventRepository;
import repository.FlashSaleItemRepository;
import repository.OrderDetailRepository;
import repository.OrderRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class OrderService {
    private static final int PURCHASE_LIMIT_PER_ITEM = 2;
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final FlashSaleItemRepository flashSaleItemRepository;
    private final FlashSaleEventRepository flashSaleEventRepository;

    public OrderService(OrderRepository orderRepository,
                        OrderDetailRepository orderDetailRepository,
                        FlashSaleItemRepository flashSaleItemRepository,
                        FlashSaleEventRepository flashSaleEventRepository) {
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.flashSaleItemRepository = flashSaleItemRepository;
        this.flashSaleEventRepository = flashSaleEventRepository;
    }

    public BookingResult placeOrderNoLock(String customerId, String flashItemId, int quantity)
            throws EntityNotFoundException, EventNotActiveException,
            ExceedPurchaseLimitException, OutOfStockException {
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

        flashSaleItemRepository.sellNoLock(flashItemId, quantity);
        FlashSaleItem updatedItem = flashSaleItemRepository.findById(flashItemId)
                .orElseThrow(() -> new EntityNotFoundException("FlashSaleItem", flashItemId));

        String orderId = nextOrderId();
        String detailId = nextDetailId();
        double totalAmount = quantity * updatedItem.getFlashPrice();

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

        return new BookingResult(order, detail, updatedItem, "Dat hang thanh cong bang NO_LOCK");
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
