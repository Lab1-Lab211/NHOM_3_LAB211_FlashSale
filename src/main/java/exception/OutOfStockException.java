package exception;

/**
 * Exception khi sản phẩm hết hàng hoặc số lượng yêu cầu vượt quá tồn kho.
 * Ném khi soldQty + soLuongYeuCau > limitedQty.
 */
public class OutOfStockException extends FlashSaleException {

    private final String flashItemId;
    private final int soLuongYeuCau;
    private final int soLuongConLai;

    public OutOfStockException(String flashItemId, int soLuongYeuCau, int soLuongConLai) {
        super(String.format("Hết hàng! Sản phẩm Flash Sale '%s': yêu cầu %d, còn lại %d",
                flashItemId, soLuongYeuCau, soLuongConLai));
        this.flashItemId = flashItemId;
        this.soLuongYeuCau = soLuongYeuCau;
        this.soLuongConLai = soLuongConLai;
    }

    public String getFlashItemId() { return flashItemId; }
    public int getSoLuongYeuCau() { return soLuongYeuCau; }
    public int getSoLuongConLai() { return soLuongConLai; }
}
