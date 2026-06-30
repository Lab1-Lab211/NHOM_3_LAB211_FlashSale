package view;

import service.BookingResult;

public class ReportView {
    public void showBookingResult(BookingResult result) {
        System.out.println();
        System.out.println("=== KET QUA BOOKING ===");
        if (result == null) {
            System.out.println("Chua co booking thanh cong.");
            return;
        }

        System.out.println(result.getMessage());
        System.out.println("Order ID      : " + result.getOrder().getOrderId());
        System.out.println("Customer ID   : " + result.getOrder().getCustomerId());
        System.out.println("Event ID      : " + result.getOrder().getEventId());
        System.out.println("Flash Item ID : " + result.getOrderDetail().getFlashItemId());
        System.out.println("Quantity      : " + result.getOrderDetail().getQuantity());
        System.out.printf("Unit price    : %.0f%n", result.getOrderDetail().getUnitPrice());
        System.out.printf("Total amount  : %.0f%n", result.getOrder().getTotalAmount());
        System.out.println("Sold quantity : " + result.getFlashSaleItem().getSoldQty());
        System.out.println("Item version  : " + result.getFlashSaleItem().getVersion());
    }
}
