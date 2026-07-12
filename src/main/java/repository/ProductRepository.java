package repository;

import model.Product;
import model.enums.ProductCategory;
import util.TextEncodingFixer;

import java.text.Normalizer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
        migrateBrokenProductNames();
    }

    @Override
    public void save(Product product) {
        repairName(product);
        super.save(product);
    }

    @Override
    public void update(Product product) {
        repairName(product);
        super.update(product);
    }

    private void repairName(Product product) {
        if (product != null) {
            product.setName(TextEncodingFixer.repairConsoleText(product.getName()));
        }
    }

    /** Tu dong sua cac ten san pham da bi console Windows luu sai truoc day. */
    private void migrateBrokenProductNames() {
        List<Product> products = findAll();
        boolean changed = false;
        for (Product product : products) {
            String repaired = TextEncodingFixer.repairConsoleText(product.getName());
            if (!repaired.equals(product.getName())) {
                product.setName(repaired);
                changed = true;
            }
        }
        if (changed) rewriteAll(products);
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
        Set<String> normalizedKeywords = searchVariants(keyword);
        return findBy(p -> matchesAny(normalizeSearchText(p.getName()), normalizedKeywords)
                || matchesAny(normalizeSearchText(p.getProductId()), normalizedKeywords));
    }

    private boolean matchesAny(String searchableText, Set<String> keywords) {
        for (String keyword : keywords) {
            if (!keyword.isEmpty() && searchableText.contains(keyword)) return true;
        }
        return false;
    }

    /**
     * Console Windows co the dua UTF-8 vao Java nhu CP437/CP1252. Tao cac bien
     * the phuc hoi de ten co dau van tim duoc, dong thoi giu tim kiem khong dau.
     */
    private Set<String> searchVariants(String value) {
        Set<String> rawVariants = new LinkedHashSet<>();
        rawVariants.add(value == null ? "" : value);
        Charset[] mistakenCharsets = {
                Charset.forName("windows-1252"),
                Charset.forName("IBM437"),
                Charset.forName("IBM850"),
                StandardCharsets.ISO_8859_1
        };
        for (Charset charset : mistakenCharsets) {
            rawVariants.add(redecodeUtf8(value, charset));
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String variant : rawVariants) {
            normalized.add(normalizeSearchText(variant));
            for (Charset charset : mistakenCharsets) {
                normalized.add(normalizeSearchText(redecodeUtf8(variant, charset)));
            }
        }
        return normalized;
    }

    private String redecodeUtf8(String value, Charset mistakenCharset) {
        if (value == null) return "";
        return new String(value.getBytes(mistakenCharset), StandardCharsets.UTF_8);
    }

    private String normalizeSearchText(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .trim();
        return normalized.replaceAll("\\s+", " ");
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
