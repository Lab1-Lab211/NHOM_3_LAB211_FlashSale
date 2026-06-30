package repository;

import model.Order;
import model.enums.OrderStatus;

import java.util.List;

/**
 * Repository quản lý đơn hàng ({@code orders.csv}).
 *
 * <p>Kế thừa {@link CsvRepository}{@code <Order>} — tái sử dụng toàn bộ CRUD.
 * Bổ sung query theo khách hàng, sự kiện, trạng thái.
 */
public class OrderRepository extends CsvRepository<Order> {

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Khởi tạo repository với đường dẫn file CSV.
     *
     * @param filePath đường dẫn tới {@code orders.csv}
     */
    public OrderRepository(String filePath) {
        super(filePath, Order::new);
    }

    // -----------------------------------------------------------------------
    // QUERY
    // -----------------------------------------------------------------------

    /**
     * Tìm tất cả đơn hàng của một khách hàng.
     *
     * @param customerId mã khách hàng (e.g. {@code "CUS-00001"})
     * @return danh sách đơn hàng của khách hàng
     */
    public List<Order> findByCustomer(String customerId) {
        return findBy(o -> o.getCustomerId().equalsIgnoreCase(customerId));
    }

    /**
     * Tìm tất cả đơn hàng trong một sự kiện Flash Sale.
     *
     * @param eventId mã sự kiện (e.g. {@code "EVT-001"})
     * @return danh sách đơn hàng thuộc sự kiện
     */
    public List<Order> findByEvent(String eventId) {
        return findBy(o -> o.getEventId().equalsIgnoreCase(eventId));
    }

    /**
     * Tìm tất cả đơn hàng theo trạng thái.
     *
     * @param status trạng thái cần lọc (CHO_XU_LY / DA_XAC_NHAN / THAT_BAI / DA_HUY)
     * @return danh sách đơn hàng có trạng thái tương ứng
     */
    public List<Order> findByStatus(OrderStatus status) {
        return findBy(o -> o.getStatus() == status);
    }

    /**
     * Tìm tất cả đơn hàng của một khách hàng trong một sự kiện cụ thể.
     * Dùng để kiểm tra giới hạn mua: mỗi khách tối đa 2 đơn vị/sản phẩm/sự kiện.
     *
     * @param customerId mã khách hàng
     * @param eventId    mã sự kiện
     * @return danh sách đơn hàng khớp cả 2 tiêu chí
     */
    public List<Order> findByCustomerAndEvent(String customerId, String eventId) {
        return findBy(o -> o.getCustomerId().equalsIgnoreCase(customerId)
                        && o.getEventId().equalsIgnoreCase(eventId));
    }
}
