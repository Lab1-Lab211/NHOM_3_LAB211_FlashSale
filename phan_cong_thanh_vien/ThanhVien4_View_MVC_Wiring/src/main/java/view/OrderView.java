package view;

import controller.CustomerController;
import controller.OrderController;
import exception.EntityNotFoundException;
import exception.EventNotActiveException;
import exception.ExceedPurchaseLimitException;
import exception.OutOfStockException;
import service.BookingResult;

import java.util.Scanner;

public class OrderView {
    private final OrderController orderController;
    private final CustomerController customerController;
    private final Scanner scanner;

    public OrderView(OrderController orderController,
                     CustomerController customerController,
                     Scanner scanner) {
        this.orderController = orderController;
        this.customerController = customerController;
        this.scanner = scanner;
    }

    public BookingResult placeOrderNoLock() {
        if (!customerController.isLoggedIn()) {
            System.out.println("Vui long login hoac register truoc.");
            return null;
        }

        System.out.print("Nhap flashItemId: ");
        String flashItemId = scanner.nextLine().trim();
        System.out.print("Nhap so luong (1-2): ");
        int quantity = readInt();

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

    private int readInt() {
        String input = scanner.nextLine().trim();
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
