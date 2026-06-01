package exception;

/**
 * Exception khi khách hàng vượt quá giới hạn mua sản phẩm trong 1 sự kiện.
 * Theo yêu cầu: mỗi khách hàng chỉ được mua tối đa 2 đơn vị cùng sản phẩm/sự kiện.
 */
public class ExceedPurchaseLimitException extends FlashSaleException {

    private final String customerId;
    private final String flashItemId;
    private final int soLuongDaMua;
    private final int gioiHanMua;

    public ExceedPurchaseLimitException(String customerId, String flashItemId,
                                        int soLuongDaMua, int gioiHanMua) {
        super(String.format("Vượt giới hạn mua! Khách '%s' đã mua %d/%d đơn vị sản phẩm '%s' trong sự kiện.",
                customerId, soLuongDaMua, gioiHanMua, flashItemId));
        this.customerId = customerId;
        this.flashItemId = flashItemId;
        this.soLuongDaMua = soLuongDaMua;
        this.gioiHanMua = gioiHanMua;
    }

    public String getCustomerId() { return customerId; }
    public String getFlashItemId() { return flashItemId; }
    public int getSoLuongDaMua() { return soLuongDaMua; }
    public int getGioiHanMua() { return gioiHanMua; }
}
