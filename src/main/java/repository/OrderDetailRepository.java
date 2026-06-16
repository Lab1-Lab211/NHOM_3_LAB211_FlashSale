package repository;

import model.OrderDetail;

import java.util.List;

/**
 * Repository quản lý chi tiết đơn hàng ({@code order_details.csv}).
 *
 * <p>Kế thừa {@link CsvRepository}{@code <OrderDetail>} — tái sử dụng toàn bộ CRUD.
 * Bổ sung query theo đơn hàng, flash item, và tính tổng tiền.
 */
public class OrderDetailRepository extends CsvRepository<OrderDetail> {

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Khởi tạo repository với đường dẫn file CSV.
     *
     * @param filePath đường dẫn tới {@code order_details.csv}
     */
    public OrderDetailRepository(String filePath) {
        super(filePath, OrderDetail::new);
    }

    // -----------------------------------------------------------------------
    // QUERY
    // -----------------------------------------------------------------------

    /**
     * Tìm tất cả chi tiết thuộc một đơn hàng.
     *
     * @param orderId mã đơn hàng (e.g. {@code "ORD-00001"})
     * @return danh sách chi tiết của đơn hàng
     */
    public List<OrderDetail> findByOrder(String orderId) {
        return findBy(d -> d.getOrderId().equalsIgnoreCase(orderId));
    }

    /**
     * Tìm tất cả chi tiết liên quan đến một flash item.
     * Dùng để kiểm tra giới hạn mua: mỗi khách tối đa 2 đơn vị/sản phẩm/sự kiện.
     *
     * @param flashItemId mã flash item (e.g. {@code "FSI-00001"})
     * @return danh sách chi tiết chứa flash item đó
     */
    public List<OrderDetail> findByFlashItem(String flashItemId) {
        return findBy(d -> d.getFlashItemId().equalsIgnoreCase(flashItemId));
    }

    /**
     * Tính tổng tiền của một đơn hàng (tổng {@code quantity × unitPrice} của các chi tiết).
     *
     * @param orderId mã đơn hàng
     * @return tổng tiền (VNĐ), trả về {@code 0} nếu không có chi tiết
     */
    public double tinhTongTien(String orderId) {
        return findByOrder(orderId).stream()
                .mapToDouble(OrderDetail::thanhTien)
                .sum();
    }

    /**
     * Đếm tổng số lượng sản phẩm (flash item) mà một khách hàng đã mua
     * trong một sự kiện cụ thể — phối hợp với OrderRepository.
     *
     * <p>Dùng bên ngoài (Controller) kết hợp:
     * <pre>{@code
     * List<Order> ordersOfCustomer = orderRepo.findByCustomerAndEvent(customerId, eventId);
     * int totalQty = 0;
     * for (Order o : ordersOfCustomer) {
     *     totalQty += detailRepo.findByOrder(o.getOrderId()).stream()
     *         .filter(d -> d.getFlashItemId().equals(flashItemId))
     *         .mapToInt(OrderDetail::getQuantity)
     *         .sum();
     * }
     * }</pre>
     *
     * @param orderId     mã đơn hàng
     * @param flashItemId mã flash item
     * @return tổng số lượng đã mua flash item trong đơn hàng này
     */
    public int soLuongDaMuaTrongDon(String orderId, String flashItemId) {
        return findByOrder(orderId).stream()
                .filter(d -> d.getFlashItemId().equalsIgnoreCase(flashItemId))
                .mapToInt(OrderDetail::getQuantity)
                .sum();
    }
}
