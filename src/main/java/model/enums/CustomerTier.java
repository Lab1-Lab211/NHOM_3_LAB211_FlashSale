package model.enums;

/**
 * Hạng thành viên của khách hàng.
 * VIP và PREMIUM được ưu tiên trong hàng đợi đặt hàng.
 */
public enum CustomerTier {
    VIP("Khách VIP", 1),
    PREMIUM("Khách Premium", 2),
    REGULAR("Khách thường", 3);

    private final String moTa;
    private final int doUuTien; // số nhỏ = ưu tiên cao

    CustomerTier(String moTa, int doUuTien) {
        this.moTa = moTa;
        this.doUuTien = doUuTien;
    }

    public String getMoTa() {
        return moTa;
    }

    public int getDoUuTien() {
        return doUuTien;
    }
}
