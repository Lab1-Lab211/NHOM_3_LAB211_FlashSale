package model;

import model.enums.LockMechanism;

/**
 * Thực thể Giao dịch Đơn hàng — ghi lại kết quả xử lý đơn hàng từ Simulator.
 * File CSV tương ứng: transactions.csv
 * File này được tạo bởi Simulator Tool khi chạy thực nghiệm so sánh cơ chế đồng bộ.
 */
public class OrderTransaction extends BaseEntity {

    private String transactionId;
    private String orderId;           // FK → orders.csv
    private LockMechanism mechanism;  // Cơ chế đồng bộ sử dụng
    private String threadName;        // Tên thread xử lý
    private long startTime;           // Thời điểm bắt đầu (nano)
    private long endTime;             // Thời điểm kết thúc (nano)
    private boolean success;          // Kết quả: thành công hay thất bại
    private String errorMessage;      // Thông báo lỗi (nếu thất bại)

    /** Constructor mặc định */
    public OrderTransaction() {
        super();
    }

    /** Constructor đầy đủ tham số */
    public OrderTransaction(String transactionId, String orderId, LockMechanism mechanism,
                            String threadName, long startTime, long endTime,
                            boolean success, String errorMessage) {
        super(transactionId);
        this.transactionId = transactionId;
        this.orderId = orderId;
        this.mechanism = mechanism;
        this.threadName = threadName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    /**
     * Tính thời gian xử lý giao dịch (mili giây).
     * @return thời gian xử lý tính bằng ms
     */
    public double thoiGianXuLyMs() {
        return (endTime - startTime) / 1_000_000.0;
    }

    @Override
    public String toCsvLine() {
        return String.join(",",
                transactionId, orderId, mechanism.name(), threadName,
                String.valueOf(startTime), String.valueOf(endTime),
                String.valueOf(success),
                errorMessage != null ? errorMessage : ""
        );
    }

    @Override
    public void fromCsvLine(String csvLine) {
        String[] parts = csvLine.split(",", -1);
        this.transactionId = parts[0].trim();
        this.id = this.transactionId;
        this.orderId = parts[1].trim();
        this.mechanism = LockMechanism.valueOf(parts[2].trim());
        this.threadName = parts[3].trim();
        this.startTime = Long.parseLong(parts[4].trim());
        this.endTime = Long.parseLong(parts[5].trim());
        this.success = Boolean.parseBoolean(parts[6].trim());
        this.errorMessage = parts.length > 7 ? parts[7].trim() : "";
    }

    @Override
    public String getCsvHeader() {
        return "transactionId,orderId,mechanism,threadName,startTime,endTime,success,errorMessage";
    }

    // === Getter & Setter ===

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; this.id = transactionId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public LockMechanism getMechanism() { return mechanism; }
    public void setMechanism(LockMechanism mechanism) { this.mechanism = mechanism; }

    public String getThreadName() { return threadName; }
    public void setThreadName(String threadName) { this.threadName = threadName; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    @Override
    public String toString() {
        return String.format("OrderTransaction{id='%s', donHang='%s', coChe=%s, thread='%s', " +
                        "thoiGian=%.2fms, thanhCong=%s}",
                transactionId, orderId, mechanism.getMoTa(), threadName,
                thoiGianXuLyMs(), success ? "Có" : "Không");
    }
}
