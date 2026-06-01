package model;

/**
 * Lớp trừu tượng cơ sở cho tất cả entity trong hệ thống.
 * Mỗi entity phải implement phương thức chuyển đổi CSV (serialize/deserialize).
 */
public abstract class BaseEntity {

    /** Mã định danh duy nhất của entity */
    protected String id;

    /** Constructor mặc định */
    public BaseEntity() {
    }

    /** Constructor với id */
    public BaseEntity(String id) {
        this.id = id;
    }

    /**
     * Chuyển đổi entity thành một dòng CSV để lưu vào file.
     * @return chuỗi CSV đại diện cho entity (các trường phân cách bằng dấu phẩy)
     */
    public abstract String toCsvLine();

    /**
     * Khởi tạo các thuộc tính của entity từ một dòng CSV.
     * @param csvLine dòng CSV cần phân tích
     */
    public abstract void fromCsvLine(String csvLine);

    /**
     * Trả về dòng tiêu đề (header) cho file CSV tương ứng.
     * @return chuỗi tiêu đề CSV
     */
    public abstract String getCsvHeader();

    // === Getter & Setter ===

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BaseEntity that = (BaseEntity) obj;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
