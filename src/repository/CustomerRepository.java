package repository;

import model.Customer;
import model.enums.CustomerTier;

import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý khách hàng ({@code customers.csv}).
 *
 * <p>Kế thừa {@link CsvRepository}{@code <Customer>} — tái sử dụng toàn bộ CRUD.
 * Bổ sung query theo email, hạng thành viên.
 */
public class CustomerRepository extends CsvRepository<Customer> {

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Khởi tạo repository với đường dẫn file CSV.
     *
     * @param filePath đường dẫn tới {@code customers.csv}
     */
    public CustomerRepository(String filePath) {
        super(filePath, Customer::new);
    }

    // -----------------------------------------------------------------------
    // QUERY
    // -----------------------------------------------------------------------

    /**
     * Tìm khách hàng theo email (unique).
     *
     * @param email địa chỉ email cần tìm
     * @return {@link Optional} chứa khách hàng nếu tìm thấy
     */
    public Optional<Customer> findByEmail(String email) {
        return findBy(c -> c.getEmail().equalsIgnoreCase(email))
                .stream().findFirst();
    }

    /**
     * Tìm tất cả khách hàng theo hạng thành viên.
     *
     * @param tier hạng thành viên (VIP / PREMIUM / REGULAR)
     * @return danh sách khách hàng thuộc hạng
     */
    public List<Customer> findByTier(CustomerTier tier) {
        return findBy(c -> c.getTier() == tier);
    }

    /**
     * Tìm khách hàng theo tên (chứa keyword, không phân biệt hoa/thường).
     *
     * @param keyword từ khóa tìm kiếm
     * @return danh sách khách hàng có tên chứa keyword
     */
    public List<Customer> findByName(String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        return findBy(c -> c.getName().toLowerCase().contains(lowerKeyword));
    }
}
