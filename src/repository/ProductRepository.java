package repository;

import model.Product;
import model.enums.ProductCategory;

import java.util.List;

/**
 * Repository quản lý sản phẩm ({@code products.csv}).
 *
 * <p>Kế thừa {@link CsvRepository}{@code <Product>} — tái sử dụng toàn bộ CRUD.
 * Bổ sung query theo danh mục, khoảng giá, tên sản phẩm.
 */
public class ProductRepository extends CsvRepository<Product> {

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Khởi tạo repository với đường dẫn file CSV.
     *
     * @param filePath đường dẫn tới {@code products.csv}
     */
    public ProductRepository(String filePath) {
        super(filePath, Product::new);
    }

    // -----------------------------------------------------------------------
    // QUERY
    // -----------------------------------------------------------------------

    /**
     * Tìm tất cả sản phẩm theo danh mục.
     *
     * @param category danh mục sản phẩm (e.g. {@code ProductCategory.DIEN_TU})
     * @return danh sách sản phẩm thuộc danh mục
     */
    public List<Product> findByCategory(ProductCategory category) {
        return findBy(p -> p.getCategory() == category);
    }

    /**
     * Tìm sản phẩm theo khoảng giá gốc.
     *
     * @param minPrice giá tối thiểu (VNĐ)
     * @param maxPrice giá tối đa (VNĐ)
     * @return danh sách sản phẩm trong khoảng giá
     */
    public List<Product> findByPriceRange(double minPrice, double maxPrice) {
        return findBy(p -> p.getOriginalPrice() >= minPrice
                        && p.getOriginalPrice() <= maxPrice);
    }

    /**
     * Tìm sản phẩm theo tên (chứa keyword, không phân biệt hoa/thường).
     *
     * @param keyword từ khóa tìm kiếm
     * @return danh sách sản phẩm có tên chứa keyword
     */
    public List<Product> findByName(String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        return findBy(p -> p.getName().toLowerCase().contains(lowerKeyword));
    }

    /**
     * Tìm sản phẩm còn tồn kho ({@code stock > 0}).
     *
     * @return danh sách sản phẩm còn hàng
     */
    public List<Product> findInStock() {
        return findBy(p -> p.getStock() > 0);
    }
}
