package model.enums;

/**
 * Hang thanh vien cua khach hang.
 * VIP va PREMIUM duoc uu tien trong hang doi dat hang.
 */
public enum CustomerTier {
    VIP("Khách VIP", 1),
    PREMIUM("Khách Premium", 2),
    REGULAR("Khách thường", 3);

    private final String moTa;
    private final int doUuTien; // so nho = uu tien cao

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
