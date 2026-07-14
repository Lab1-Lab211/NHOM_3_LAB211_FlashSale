package controller;

import exception.EntityNotFoundException;
import exception.EventNotActiveException;
import exception.ExceedPurchaseLimitException;
import exception.OptimisticLockException;
import exception.OutOfStockException;
import model.Customer;
import model.enums.LockMechanism;
import service.BookingResult;
import service.OrderService;

import java.util.List;
import model.Order;

public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
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
        return orderService.placeOrder(customer, flashItemId, quantity, mechanism);
    }

    public BookingResult placeNormalOrder(Customer customer, String productId, int quantity)
            throws exception.EntityNotFoundException, exception.OutOfStockException {
        if (customer == null) {
            throw new IllegalStateException("Vui long login truoc khi dat hang");
        }
        return orderService.placeNormalOrder(customer, productId, quantity);
    }

    public List<Order> getOrders(Customer customer) {
        return orderService.getOrdersForCustomer(customer);
    }

    public BookingResult placeRegularProductOrder(Customer customer, String productId, int quantity)
            throws EntityNotFoundException, OutOfStockException {
        return orderService.placeRegularProductOrder(customer, productId, quantity);
    }

    public Order cancelOrder(Customer customer, String orderId) throws EntityNotFoundException {
        return orderService.cancelOrder(customer, orderId);
    }
}
