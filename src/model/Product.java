package model;

import model.enums.ProductCategory;

/**
 * Thực thể Sản phẩm — đại diện cho một sản phẩm trong hệ thống.
 * File CSV tương ứng: products.csv
 */
public class Product extends BaseEntity {

    private String productId;
    private String name;              // Tên sản phẩm
    private ProductCategory category; // Danh mục
    private double originalPrice;     // Giá gốc (VNĐ)
    private int stock;                // Tồn kho hiện tại
    private int version;              // Phiên bản cho Optimistic Lock

    /** Constructor mặc định — dùng cho fromCsvLine() */
    public Product() {
        super();
    }

    /** Constructor đầy đủ tham số */
    public Product(String productId, String name, ProductCategory category,
                   double originalPrice, int stock, int version) {
        super(productId);
        this.productId = productId;
        this.name = name;
        this.category = category;
        this.originalPrice = originalPrice;
        this.stock = stock;
        this.version = version;
    }

    /**
     * Chuyển đổi đối tượng Product thành dòng CSV.
     * Định dạng: productId,name,category,originalPrice,stock,version
     */
    @Override
    public String toCsvLine() {
        return String.join(",",
                productId,
                name,
                category.name(),
                String.valueOf(originalPrice),
                String.valueOf(stock),
                String.valueOf(version)
        );
    }

    /**
     * Phân tích dòng CSV và gán giá trị cho các thuộc tính.
     * @param csvLine dòng CSV cần phân tích
     */
    @Override
    public void fromCsvLine(String csvLine) {
        String[] parts = csvLine.split(",", -1);
        this.productId = parts[0].trim();
        this.id = this.productId;
        this.name = parts[1].trim();
        this.category = ProductCategory.valueOf(parts[2].trim());
        this.originalPrice = Double.parseDouble(parts[3].trim());
        this.stock = Integer.parseInt(parts[4].trim());
        this.version = Integer.parseInt(parts[5].trim());
    }

    /** Trả về dòng tiêu đề CSV */
    @Override
    public String getCsvHeader() {
        return "productId,name,category,originalPrice,stock,version";
    }

    // === Getter & Setter ===

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; this.id = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ProductCategory getCategory() { return category; }
    public void setCategory(ProductCategory category) { this.category = category; }

    public double getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(double originalPrice) { this.originalPrice = originalPrice; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    @Override
    public String toString() {
        return String.format("Product{id='%s', ten='%s', danhMuc=%s, giaGoc=%.0f, tonKho=%d, version=%d}",
                productId, name, category.getMoTa(), originalPrice, stock, version);
    }
}
