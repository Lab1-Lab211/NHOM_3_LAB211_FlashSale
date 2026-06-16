package model.enums;

/**
 * Danh mục sản phẩm trong hệ thống thương mại điện tử.
 */
public enum ProductCategory {
    DIEN_TU("Điện tử"),
    THOI_TRANG("Thời trang"),
    GIA_DUNG("Gia dụng"),
    LAM_DEP("Làm đẹp"),
    THUC_PHAM("Thực phẩm"),
    THE_THAO("Thể thao");

    private final String moTa;

    ProductCategory(String moTa) {
        this.moTa = moTa;
    }

    public String getMoTa() {
        return moTa;
    }
}
