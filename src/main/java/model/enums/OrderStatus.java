package model.enums;

/**
 * Trạng thái của đơn hàng.
 */
public enum OrderStatus {
    CHO_XU_LY("Chờ xử lý"),
    DA_XAC_NHAN("Đã xác nhận"),
    DANG_CHUAN_BI("Đang chuẩn bị hàng"),
    DANG_GIAO("Đang giao hàng"),
    HOAN_THANH("Hoàn thành"),
    THAT_BAI("Thất bại"),
    DA_HUY("Đã hủy");

    private final String moTa;

    OrderStatus(String moTa) {
        this.moTa = moTa;
    }

    public String getMoTa() {
        return moTa;
    }
}
