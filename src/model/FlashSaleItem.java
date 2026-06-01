package model;

/**
 * Thực thể Sản phẩm Flash Sale — đại diện cho một sản phẩm tham gia sự kiện Flash Sale.
 * ĐÂY LÀ ENTITY QUAN TRỌNG NHẤT — chứa trường version cho Optimistic Lock.
 *
 * Bất biến (Invariant): soldQty <= limitedQty — KHÔNG BAO GIỜ ĐƯỢC VI PHẠM.
 * Trường version tăng dần mỗi lần soldQty được cập nhật.
 *
 * File CSV tương ứng: flash_items.csv
 */
public class FlashSaleItem extends BaseEntity {

    private String flashItemId;
    private String eventId;           // FK → flash_events.csv
    private String productId;         // FK → products.csv
    private int limitedQty;           // Số lượng giới hạn bán ra
    private int soldQty;              // Số lượng đã bán (soldQty <= limitedQty)
    private double flashPrice;        // Giá Flash Sale (đã giảm)
    private int version;              // Phiên bản — nền tảng Optimistic Lock

    /** Constructor mặc định */
    public FlashSaleItem() {
        super();
    }

    /** Constructor đầy đủ tham số */
    public FlashSaleItem(String flashItemId, String eventId, String productId,
                         int limitedQty, int soldQty, double flashPrice, int version) {
        super(flashItemId);
        this.flashItemId = flashItemId;
        this.eventId = eventId;
        this.productId = productId;
        this.limitedQty = limitedQty;
        this.soldQty = soldQty;
        this.flashPrice = flashPrice;
        this.version = version;
    }

    /**
     * Kiểm tra xem có thể bán thêm số lượng yêu cầu hay không.
     * @param soLuongYeuCau số lượng muốn mua
     * @return true nếu soldQty + soLuongYeuCau <= limitedQty
     */
    public boolean coBanDuoc(int soLuongYeuCau) {
        return (soldQty + soLuongYeuCau) <= limitedQty;
    }

    /**
     * Trả về số lượng còn lại có thể bán.
     * @return limitedQty - soldQty
     */
    public int soLuongConLai() {
        return limitedQty - soldQty;
    }

    @Override
    public String toCsvLine() {
        return String.join(",",
                flashItemId, eventId, productId,
                String.valueOf(limitedQty),
                String.valueOf(soldQty),
                String.valueOf(flashPrice),
                String.valueOf(version)
        );
    }

    @Override
    public void fromCsvLine(String csvLine) {
        String[] parts = csvLine.split(",", -1);
        this.flashItemId = parts[0].trim();
        this.id = this.flashItemId;
        this.eventId = parts[1].trim();
        this.productId = parts[2].trim();
        this.limitedQty = Integer.parseInt(parts[3].trim());
        this.soldQty = Integer.parseInt(parts[4].trim());
        this.flashPrice = Double.parseDouble(parts[5].trim());
        this.version = Integer.parseInt(parts[6].trim());
    }

    @Override
    public String getCsvHeader() {
        return "flashItemId,eventId,productId,limitedQty,soldQty,flashPrice,version";
    }

    // === Getter & Setter ===

    public String getFlashItemId() { return flashItemId; }
    public void setFlashItemId(String flashItemId) { this.flashItemId = flashItemId; this.id = flashItemId; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public int getLimitedQty() { return limitedQty; }
    public void setLimitedQty(int limitedQty) { this.limitedQty = limitedQty; }

    public int getSoldQty() { return soldQty; }
    public void setSoldQty(int soldQty) { this.soldQty = soldQty; }

    public double getFlashPrice() { return flashPrice; }
    public void setFlashPrice(double flashPrice) { this.flashPrice = flashPrice; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    @Override
    public String toString() {
        return String.format("FlashSaleItem{id='%s', eventId='%s', productId='%s', " +
                        "gioiHan=%d, daBan=%d, conLai=%d, giaFlash=%.0f, version=%d}",
                flashItemId, eventId, productId, limitedQty, soldQty,
                soLuongConLai(), flashPrice, version);
    }
}
