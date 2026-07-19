package repository;

import model.FlashSaleEvent;
import model.enums.SaleStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Repository quản lý sự kiện Flash Sale ({@code flash_events.csv}).
 *
 * <p>Kế thừa {@link CsvRepository}{@code <FlashSaleEvent>} — tái sử dụng toàn bộ CRUD.
 * Bổ sung query theo trạng thái sự kiện.
 */
public class FlashSaleEventRepository extends CsvRepository<FlashSaleEvent> {

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Khởi tạo repository với đường dẫn file CSV.
     *
     * @param filePath đường dẫn tới {@code flash_events.csv}
     */
    public FlashSaleEventRepository(String filePath) {
        super(filePath, FlashSaleEvent::new);
    }

    /**
     * Đồng bộ các sự kiện sắp diễn ra theo thời gian mỗi khi dữ liệu được đọc.
     * Khi startTime đã tới, trạng thái tự chuyển từ SAP_DIEN_RA sang DANG_DIEN_RA.
     */
    @Override
    public synchronized List<FlashSaleEvent> findAll() {
        List<FlashSaleEvent> events = super.findAll();
        LocalDateTime now = LocalDateTime.now();
        boolean changed = false;

        for (FlashSaleEvent event : events) {
            if (event.getStatus() != SaleStatus.SAP_DIEN_RA) continue;
            try {
                LocalDateTime startTime = LocalDateTime.parse(event.getStartTime());
                if (!now.isBefore(startTime)) {
                    event.setStatus(SaleStatus.DANG_DIEN_RA);
                    changed = true;
                }
            } catch (DateTimeParseException ignored) {
                // Giữ nguyên trạng thái nếu dữ liệu thời gian cũ không đúng định dạng.
            }
        }

        if (changed) {
            rewriteAll(events);
        }
        return events;
    }

    // -----------------------------------------------------------------------
    // QUERY
    // -----------------------------------------------------------------------

    /**
     * Tìm tất cả sự kiện theo trạng thái.
     *
     * @param status trạng thái cần lọc (SAP_DIEN_RA / DANG_DIEN_RA / DA_KET_THUC)
     * @return danh sách sự kiện có trạng thái tương ứng
     */
    public List<FlashSaleEvent> findByStatus(SaleStatus status) {
        return findBy(e -> e.getStatus() == status);
    }

    /**
     * Tìm tất cả sự kiện đang diễn ra ({@code DANG_DIEN_RA}).
     * Convenience method — chỉ cho phép đặt hàng trong các sự kiện này.
     *
     * @return danh sách sự kiện đang diễn ra
     */
    public List<FlashSaleEvent> findDangDienRa() {
        return findByStatus(SaleStatus.DANG_DIEN_RA);
    }

    /**
     * Tìm tất cả sự kiện sắp diễn ra ({@code SAP_DIEN_RA}).
     *
     * @return danh sách sự kiện sắp diễn ra
     */
    public List<FlashSaleEvent> findSapDienRa() {
        return findByStatus(SaleStatus.SAP_DIEN_RA);
    }

    /**
     * Tìm tất cả sự kiện đã kết thúc ({@code DA_KET_THUC}).
     *
     * @return danh sách sự kiện đã kết thúc
     */
    public List<FlashSaleEvent> findDaKetThuc() {
        return findByStatus(SaleStatus.DA_KET_THUC);
    }

    /**
     * Tìm sự kiện theo tên (chứa keyword, không phân biệt hoa/thường).
     *
     * @param keyword từ khóa tìm kiếm
     * @return danh sách sự kiện có tên chứa keyword
     */
    public List<FlashSaleEvent> findByName(String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        return findBy(e -> e.getEventName().toLowerCase().contains(lowerKeyword));
    }
}
