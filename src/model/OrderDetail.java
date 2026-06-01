package model;

/**
 * Thực thể Chi tiết Đơn hàng — mỗi dòng đại diện cho một sản phẩm trong đơn.
 * File CSV tương ứng: order_details.csv
 */
public class OrderDetail extends BaseEntity {

    private String detailId;
    private String orderId;           // FK → orders.csv
    private String flashItemId;       // FK → flash_items.csv
    private int quantity;             // Số lượng mua (tối đa 2 mỗi sản phẩm/sự kiện)
    private double unitPrice;         // Đơn giá tại thời điểm mua (VNĐ)

    /** Constructor mặc định */
    public OrderDetail() {
        super();
    }

    /** Constructor đầy đủ tham số */
    public OrderDetail(String detailId, String orderId, String flashItemId,
                       int quantity, double unitPrice) {
        super(detailId);
        this.detailId = detailId;
        this.orderId = orderId;
        this.flashItemId = flashItemId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    /**
     * Tính thành tiền cho chi tiết đơn hàng này.
     * @return quantity × unitPrice
     */
    public double thanhTien() {
        return quantity * unitPrice;
    }

    @Override
    public String toCsvLine() {
        return String.join(",",
                detailId, orderId, flashItemId,
                String.valueOf(quantity), String.valueOf(unitPrice)
        );
    }

    @Override
    public void fromCsvLine(String csvLine) {
        String[] parts = csvLine.split(",", -1);
        this.detailId = parts[0].trim();
        this.id = this.detailId;
        this.orderId = parts[1].trim();
        this.flashItemId = parts[2].trim();
        this.quantity = Integer.parseInt(parts[3].trim());
        this.unitPrice = Double.parseDouble(parts[4].trim());
    }

    @Override
    public String getCsvHeader() {
        return "detailId,orderId,flashItemId,quantity,unitPrice";
    }

    // === Getter & Setter ===

    public String getDetailId() { return detailId; }
    public void setDetailId(String detailId) { this.detailId = detailId; this.id = detailId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getFlashItemId() { return flashItemId; }
    public void setFlashItemId(String flashItemId) { this.flashItemId = flashItemId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    @Override
    public String toString() {
        return String.format("OrderDetail{id='%s', donHang='%s', sanPham='%s', soLuong=%d, donGia=%.0f, thanhTien=%.0f}",
                detailId, orderId, flashItemId, quantity, unitPrice, thanhTien());
    }
}
