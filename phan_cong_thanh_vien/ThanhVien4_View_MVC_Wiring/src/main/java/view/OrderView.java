package view;

import controller.CustomerController;
import controller.OrderController;
import exception.EntityNotFoundException;
import exception.EventNotActiveException;
import exception.ExceedPurchaseLimitException;
import exception.OutOfStockException;
import service.BookingResult;

public class OrderView {
    private final OrderController orderController;
    private final CustomerController customerController;
    private final ConsoleInput input;

    public OrderView(OrderController orderController,
                     CustomerController customerController,
                     ConsoleInput input) {
        this.orderController = orderController;
        this.customerController = customerController;
        this.input = input;
    }

    public BookingResult placeOrderNoLock() {
        if (!customerController.isLoggedIn()) {
            System.out.println("Vui long login hoac register truoc.");
            return null;
        }

        String flashItemId = input.readLine("Nhap flashItemId: ").trim();
        int quantity = input.readInt("Nhap so luong (1-2): ");

        try {
            return orderController.placeOrderNoLock(
                    customerController.getCurrentCustomer(), flashItemId, quantity);
        } catch (EntityNotFoundException | EventNotActiveException
                 | ExceedPurchaseLimitException | OutOfStockException
                 | IllegalArgumentException | IllegalStateException e) {
            System.out.println("Dat hang that bai: " + e.getMessage());
            return null;
        }
    }

}
