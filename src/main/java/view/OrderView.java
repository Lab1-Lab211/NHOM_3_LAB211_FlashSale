package view;

import controller.CustomerController;
import controller.OrderController;
import exception.FlashSaleException;
import model.enums.LockMechanism;
import service.BookingResult;
import model.Order;

import java.util.List;

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

    public BookingResult placeOrderSafely() {
        if (!customerController.isLoggedIn()) {
            System.out.println("Vui long login hoac register truoc.");
            return null;
        }
        System.out.println("1. Dat san pham Flash Sale (FSI-...)");
        System.out.println("2. Dat san pham thuong (PRD-...)");
        int choice = input.readInt("Chon loai don hang: ");
        if (choice == 1) return placeOrder(LockMechanism.SYNCHRONIZED);
        if (choice == 2) return placeRegularProductOrder();
        System.out.println("Loai don hang khong hop le.");
        return null;
    }

    public void showMyOrders() {
        if (!customerController.isLoggedIn()) {
            System.out.println("Vui long login de xem don hang.");
            return;
        }
        List<Order> orders = orderController.getOrders(customerController.getCurrentCustomer());
        if (orders.isEmpty()) {
            System.out.println("Ban chua co don hang nao.");
            return;
        }
        System.out.printf("%-12s %-12s %-20s %-18s %12s%n",
                "Order ID", "Event ID", "Thoi gian", "Trang thai", "Tong tien");
        for (Order order : orders) {
            System.out.printf("%-12s %-12s %-20s %-18s %12.0f%n",
                    order.getOrderId(), order.getEventId(), order.getOrderTime(),
                    order.getStatus().getMoTa(), order.getTotalAmount());
        }
    }

    public void cancelMyOrder() {
        if (!customerController.isLoggedIn()) {
            System.out.println("Vui long login de huy don hang.");
            return;
        }
        String orderId = input.readLine("Nhap Order ID can huy: ").trim();
        try {
            Order order = orderController.cancelOrder(customerController.getCurrentCustomer(), orderId);
            System.out.println("Huy don thanh cong. Trang thai: " + order.getStatus().getMoTa());
        } catch (FlashSaleException | IllegalArgumentException | IllegalStateException e) {
            System.out.println("Huy don that bai: " + e.getMessage());
        }
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

    private BookingResult placeRegularProductOrder() {
        String productId = input.readLine("Nhap productId (PRD-...): ").trim();
        int quantity = input.readInt("Nhap so luong: ");
        try {
            return orderController.placeRegularProductOrder(
                    customerController.getCurrentCustomer(), productId, quantity);
        } catch (FlashSaleException | IllegalArgumentException | IllegalStateException e) {
            System.out.println("Dat san pham thuong that bai: " + e.getMessage());
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
