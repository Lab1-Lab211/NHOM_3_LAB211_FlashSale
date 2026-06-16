package repository;

import model.OrderTransaction;
import model.enums.LockMechanism;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Repository quản lý giao dịch đơn hàng ({@code transactions.csv}).
 *
 * <p>File này được sinh bởi <b>Simulator Tool</b> khi chạy thực nghiệm
 * so sánh cơ chế đồng bộ — không được sinh bởi {@code DataGenerator}.
 *
 * <p>Kế thừa {@link CsvRepository}{@code <OrderTransaction>} — tái sử dụng toàn bộ CRUD.
 * Bổ sung query theo mechanism, thống kê throughput TPS.
 */
public class OrderTransactionRepository extends CsvRepository<OrderTransaction> {

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Khởi tạo repository với đường dẫn file CSV.
     *
     * @param filePath đường dẫn tới {@code transactions.csv}
     */
    public OrderTransactionRepository(String filePath) {
        super(filePath, OrderTransaction::new);
    }

    // -----------------------------------------------------------------------
    // QUERY
    // -----------------------------------------------------------------------

    /**
     * Tìm tất cả giao dịch theo cơ chế đồng bộ.
     *
     * @param mechanism cơ chế cần lọc (NO_LOCK / FILE_LOCK / SYNCHRONIZED / OPTIMISTIC)
     * @return danh sách giao dịch dùng cơ chế đó
     */
    public List<OrderTransaction> findByMechanism(LockMechanism mechanism) {
        return findBy(t -> t.getMechanism() == mechanism);
    }

    /**
     * Tìm tất cả giao dịch thành công hoặc thất bại.
     *
     * @param success {@code true} để lọc thành công, {@code false} để lọc thất bại
     * @return danh sách giao dịch theo kết quả
     */
    public List<OrderTransaction> findBySuccess(boolean success) {
        return findBy(t -> t.isSuccess() == success);
    }

    /**
     * Tìm tất cả giao dịch theo cơ chế VÀ kết quả.
     *
     * @param mechanism cơ chế đồng bộ
     * @param success   kết quả mong muốn
     * @return danh sách giao dịch thỏa cả 2 điều kiện
     */
    public List<OrderTransaction> findByMechanismAndSuccess(LockMechanism mechanism, boolean success) {
        return findBy(t -> t.getMechanism() == mechanism && t.isSuccess() == success);
    }

    // -----------------------------------------------------------------------
    // THỐNG KÊ — Dùng cho Simulator Report
    // -----------------------------------------------------------------------

    /**
     * Tính Throughput (TPS — Transactions Per Second) cho một cơ chế.
     *
     * <p>Công thức: {@code TPS = successCount / wallClockSeconds}
     * <br>Trong đó {@code wallClockSeconds} = (max endTime - min startTime) / 1_000_000_000.0
     *
     * @param mechanism cơ chế cần đo
     * @return TPS (đơn/giây), trả về 0 nếu không có dữ liệu
     */
    public double tinhThroughput(LockMechanism mechanism) {
        List<OrderTransaction> txns = findByMechanism(mechanism);
        if (txns.isEmpty()) return 0;

        long successCount = txns.stream().filter(OrderTransaction::isSuccess).count();

        long minStart = txns.stream().mapToLong(OrderTransaction::getStartTime).min().orElse(0);
        long maxEnd   = txns.stream().mapToLong(OrderTransaction::getEndTime).max().orElse(0);

        double wallClockSeconds = (maxEnd - minStart) / 1_000_000_000.0;
        if (wallClockSeconds <= 0) return 0;

        return successCount / wallClockSeconds;
    }

    /**
     * Tính latency trung bình (mili giây) cho một cơ chế.
     *
     * @param mechanism cơ chế cần đo
     * @return latency trung bình (ms), trả về 0 nếu không có dữ liệu
     */
    public double tinhAvgLatencyMs(LockMechanism mechanism) {
        List<OrderTransaction> txns = findByMechanism(mechanism);
        if (txns.isEmpty()) return 0;

        return txns.stream()
                .mapToDouble(OrderTransaction::thoiGianXuLyMs)
                .average()
                .orElse(0);
    }

    /**
     * Đếm số giao dịch thành công cho một cơ chế.
     *
     * @param mechanism cơ chế cần đếm
     * @return số giao dịch thành công
     */
    public long demThanhCong(LockMechanism mechanism) {
        return findByMechanismAndSuccess(mechanism, true).size();
    }

    /**
     * Đếm số giao dịch thất bại cho một cơ chế.
     *
     * @param mechanism cơ chế cần đếm
     * @return số giao dịch thất bại
     */
    public long demThatBai(LockMechanism mechanism) {
        return findByMechanismAndSuccess(mechanism, false).size();
    }

    // -----------------------------------------------------------------------
    // MAINTENANCE
    // -----------------------------------------------------------------------

    /**
     * Xóa toàn bộ dữ liệu giao dịch, chỉ giữ header.
     * Dùng trước mỗi đợt simulation mới.
     */
    public void clearAll() {
        rewriteAll(java.util.Collections.emptyList());
    }
}
