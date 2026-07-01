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
}
