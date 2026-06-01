package model;

import model.enums.OrderStatus;

/**
 * Thực thể Đơn hàng — đại diện cho một đơn đặt hàng của khách hàng.
 * File CSV tương ứng: orders.csv
 */
public class Order extends BaseEntity {

    private String orderId;
    private String customerId;        // FK → customers.csv
    private String eventId;           // FK → flash_events.csv
    private String orderTime;         // Thời gian đặt hàng (yyyy-MM-dd'T'HH:mm:ss)
    private OrderStatus status;       // Trạng thái đơn hàng
    private double totalAmount;       // Tổng tiền (VNĐ)

    /** Constructor mặc định */
    public Order() {
        super();
    }

    /** Constructor đầy đủ tham số */
    public Order(String orderId, String customerId, String eventId,
                 String orderTime, OrderStatus status, double totalAmount) {
        super(orderId);
        this.orderId = orderId;
        this.customerId = customerId;
        this.eventId = eventId;
        this.orderTime = orderTime;
        this.status = status;
        this.totalAmount = totalAmount;
    }

    @Override
    public String toCsvLine() {
        return String.join(",",
                orderId, customerId, eventId, orderTime,
                status.name(), String.valueOf(totalAmount)
        );
    }

    @Override
    public void fromCsvLine(String csvLine) {
        String[] parts = csvLine.split(",", -1);
        this.orderId = parts[0].trim();
        this.id = this.orderId;
        this.customerId = parts[1].trim();
        this.eventId = parts[2].trim();
        this.orderTime = parts[3].trim();
        this.status = OrderStatus.valueOf(parts[4].trim());
        this.totalAmount = Double.parseDouble(parts[5].trim());
    }

    @Override
    public String getCsvHeader() {
        return "orderId,customerId,eventId,orderTime,status,totalAmount";
    }

    // === Getter & Setter ===

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; this.id = orderId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getOrderTime() { return orderTime; }
    public void setOrderTime(String orderTime) { this.orderTime = orderTime; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    @Override
    public String toString() {
        return String.format("Order{id='%s', khachHang='%s', suKien='%s', trangThai=%s, tongTien=%.0f}",
                orderId, customerId, eventId, status.getMoTa(), totalAmount);
    }
}
