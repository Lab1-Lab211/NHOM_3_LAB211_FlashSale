package model.enums;

/**
 * Phương thức thanh toán đơn hàng.
 */
public enum PaymentMethod {
    THE_TIN_DUNG("Thẻ tín dụng"),
    VI_DIEN_TU("Ví điện tử"),
    THANH_TOAN_KHI_NHAN("Thanh toán khi nhận hàng");

    private final String moTa;

    PaymentMethod(String moTa) {
        this.moTa = moTa;
    }

    public String getMoTa() {
        return moTa;
    }
}
