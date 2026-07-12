package model.enums;

/**
 * Trạng thái của sự kiện Flash Sale.
 */
public enum SaleStatus {
    CHO_PHE_DUYET("Cho phe duyet"),
    TU_CHOI("Bi tu choi"),
    SAP_DIEN_RA("Sắp diễn ra"),
    DANG_DIEN_RA("Đang diễn ra"),
    DA_KET_THUC("Đã kết thúc");

    private final String moTa;

    SaleStatus(String moTa) {
        this.moTa = moTa;
    }

    public String getMoTa() {
        return moTa;
    }
}
