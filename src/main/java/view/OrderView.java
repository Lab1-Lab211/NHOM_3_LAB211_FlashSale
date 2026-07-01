package view;

import controller.CustomerController;
import controller.OrderController;
import exception.FlashSaleException;
import model.enums.LockMechanism;
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
        return placeOrder(LockMechanism.NO_LOCK);
    }

    public BookingResult placeOrderWithSelectedMechanism() {
        LockMechanism mechanism = readMechanism();
        return placeOrder(mechanism);
    }

    private BookingResult placeOrder(LockMechanism mechanism) {
        if (!customerController.isLoggedIn()) {
            System.out.println("Vui long login hoac register truoc.");
            return null;
        }

        String flashItemId = input.readLine("Nhap flashItemId: ").trim();
        int quantity = input.readInt("Nhap so luong (1-2): ");

        try {
            return orderController.placeOrder(
                    customerController.getCurrentCustomer(), flashItemId, quantity, mechanism);
        } catch (FlashSaleException | IllegalArgumentException | IllegalStateException e) {
            System.out.println("Dat hang that bai: " + e.getMessage());
            return null;
        }
    }

    private LockMechanism readMechanism() {
        System.out.println("Chon co che dat hang:");
        LockMechanism[] values = LockMechanism.values();
        for (int i = 0; i < values.length; i++) {
            System.out.printf("%d. %s - %s%n", i + 1, values[i].name(), values[i].getMoTa());
        }
        int choice = input.readInt("Lua chon (1-4): ");
        if (choice < 1 || choice > values.length) {
            System.out.println("Lua chon khong hop le, fallback NO_LOCK.");
            return LockMechanism.NO_LOCK;
        }
        return values[choice - 1];
    }
}
