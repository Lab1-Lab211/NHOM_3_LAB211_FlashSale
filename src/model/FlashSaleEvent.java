package model;

import model.enums.SaleStatus;

/**
 * Thực thể Sự kiện Flash Sale — đại diện cho một đợt giảm giá chớp nhoáng.
 * File CSV tương ứng: flash_events.csv
 */
public class FlashSaleEvent extends BaseEntity {

    private String eventId;
    private String eventName;         // Tên sự kiện
    private String startTime;         // Thời gian bắt đầu (yyyy-MM-dd'T'HH:mm:ss)
    private String endTime;           // Thời gian kết thúc
    private SaleStatus status;        // Trạng thái sự kiện
    private int discountPercent;      // Phần trăm giảm giá (30-70%)

    /** Constructor mặc định */
    public FlashSaleEvent() {
        super();
    }

    /** Constructor đầy đủ tham số */
    public FlashSaleEvent(String eventId, String eventName, String startTime,
                          String endTime, SaleStatus status, int discountPercent) {
        super(eventId);
        this.eventId = eventId;
        this.eventName = eventName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.discountPercent = discountPercent;
    }

    @Override
    public String toCsvLine() {
        return String.join(",",
                eventId, eventName, startTime, endTime, status.name(),
                String.valueOf(discountPercent)
        );
    }

    @Override
    public void fromCsvLine(String csvLine) {
        String[] parts = csvLine.split(",", -1);
        this.eventId = parts[0].trim();
        this.id = this.eventId;
        this.eventName = parts[1].trim();
        this.startTime = parts[2].trim();
        this.endTime = parts[3].trim();
        this.status = SaleStatus.valueOf(parts[4].trim());
        this.discountPercent = Integer.parseInt(parts[5].trim());
    }

    @Override
    public String getCsvHeader() {
        return "eventId,eventName,startTime,endTime,status,discountPercent";
    }

    // === Getter & Setter ===

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; this.id = eventId; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public SaleStatus getStatus() { return status; }
    public void setStatus(SaleStatus status) { this.status = status; }

    public int getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(int discountPercent) { this.discountPercent = discountPercent; }

    @Override
    public String toString() {
        return String.format("FlashSaleEvent{id='%s', ten='%s', trangThai=%s, giamGia=%d%%}",
                eventId, eventName, status.getMoTa(), discountPercent);
    }
}
