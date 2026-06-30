package service;

import model.FlashSaleItem;
import model.Order;
import model.OrderDetail;
import model.enums.CustomerTier;

public class BookingResult {
    private final Order order;
    private final OrderDetail orderDetail;
    private final FlashSaleItem flashSaleItem;
    private final String message;
    private final CustomerTier tierBeforeOrder;
    private final CustomerTier tierAfterOrder;
    private final double subtotalAmount;
    private final double discountPercent;
    private final double discountAmount;

    public BookingResult(Order order, OrderDetail orderDetail, FlashSaleItem flashSaleItem, String message) {
        this(order, orderDetail, flashSaleItem, message, null, null,
                order != null ? order.getTotalAmount() : 0, 0, 0);
    }

    public BookingResult(Order order, OrderDetail orderDetail, FlashSaleItem flashSaleItem, String message,
                         CustomerTier tierBeforeOrder, CustomerTier tierAfterOrder,
                         double subtotalAmount, double discountPercent, double discountAmount) {
        this.order = order;
        this.orderDetail = orderDetail;
        this.flashSaleItem = flashSaleItem;
        this.message = message;
        this.tierBeforeOrder = tierBeforeOrder;
        this.tierAfterOrder = tierAfterOrder;
        this.subtotalAmount = subtotalAmount;
        this.discountPercent = discountPercent;
        this.discountAmount = discountAmount;
    }

    public Order getOrder() {
        return order;
    }

    public OrderDetail getOrderDetail() {
        return orderDetail;
    }

    public FlashSaleItem getFlashSaleItem() {
        return flashSaleItem;
    }

    public String getMessage() {
        return message;
    }

    public CustomerTier getTierBeforeOrder() {
        return tierBeforeOrder;
    }

    public CustomerTier getTierAfterOrder() {
        return tierAfterOrder;
    }

    public double getSubtotalAmount() {
        return subtotalAmount;
    }

    public double getDiscountPercent() {
        return discountPercent;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }
}
