package service;

import model.FlashSaleItem;
import model.Order;
import model.OrderDetail;

public class BookingResult {
    private final Order order;
    private final OrderDetail orderDetail;
    private final FlashSaleItem flashSaleItem;
    private final String message;

    public BookingResult(Order order, OrderDetail orderDetail, FlashSaleItem flashSaleItem, String message) {
        this.order = order;
        this.orderDetail = orderDetail;
        this.flashSaleItem = flashSaleItem;
        this.message = message;
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
}
