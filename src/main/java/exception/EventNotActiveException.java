package exception;

/**
 * Exception khi cố gắng đặt hàng trong sự kiện Flash Sale chưa bắt đầu hoặc đã kết thúc.
 * Chỉ được đặt hàng khi sự kiện có trạng thái DANG_DIEN_RA.
 */
public class EventNotActiveException extends FlashSaleException {

    private final String eventId;
    private final String trangThaiHienTai;

    public EventNotActiveException(String eventId, String trangThaiHienTai) {
        super(String.format("Sự kiện Flash Sale '%s' không hoạt động! Trạng thái hiện tại: %s. "
                + "Chỉ có thể đặt hàng khi sự kiện ĐANG DIỄN RA.", eventId, trangThaiHienTai));
        this.eventId = eventId;
        this.trangThaiHienTai = trangThaiHienTai;
    }

    public String getEventId() { return eventId; }
    public String getTrangThaiHienTai() { return trangThaiHienTai; }
}
