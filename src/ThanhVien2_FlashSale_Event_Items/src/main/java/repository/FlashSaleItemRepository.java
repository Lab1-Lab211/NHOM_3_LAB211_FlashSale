package repository;

import model.FlashSaleItem;
import exception.EntityNotFoundException;
import exception.OptimisticLockException;
import exception.OutOfStockException;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository quản lý tồn kho Flash Sale Item ({@code flash_items.csv}).
 *
 * <p>
 * <b>ĐÂY LÀ REPOSITORY QUAN TRỌNG NHẤT</b> — chứa logic trừ kho với 4 cơ chế
 * đồng bộ:
 * <ol>
 * <li>{@link #sellNoLock} — Baseline, không khóa → gây race condition (âm
 * kho)</li>
 * <li>{@link #sellWithSynchronized} — {@code synchronized} method</li>
 * <li>{@link #sellWithFileLock} — {@code java.nio.channels.FileLock} NIO</li>
 * <li>{@link #sellWithOptimisticLock} — version field + retry loop
 * ({@code MAX_RETRY=3})</li>
 * </ol>
 *
 * <p>
 * Bất biến cốt lõi: {@code soldQty ≤ limitedQty} — KHÔNG BAO GIỜ ĐƯỢC VI PHẠM
 * (trừ khi dùng NO_LOCK trong race condition).
 *
 * <p>
 * ⚠ Toàn bộ logic trừ kho nằm trong Repository — không được rò ra Controller
 * hay View
 * (theo yêu cầu MVC của đề bài).
 */
public class FlashSaleItemRepository extends CsvRepository<FlashSaleItem> {

    /** Số lần retry tối đa cho Optimistic Lock. */
    private static final int MAX_RETRY = 3;

    /** Lock object cho compare-and-swap trong Optimistic Lock. */
    private final Object optimisticWriteLock = new Object();

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Khởi tạo repository với đường dẫn file CSV.
     *
     * @param filePath đường dẫn tới {@code flash_items.csv}
     */
    public FlashSaleItemRepository(String filePath) {
        super(filePath, FlashSaleItem::new);
    }

    // -----------------------------------------------------------------------
    // QUERY HELPERS
    // -----------------------------------------------------------------------

    /**
     * Tìm tất cả flash item thuộc một sự kiện.
     *
     * @param eventId mã sự kiện (e.g. {@code "EVT-001"})
     * @return danh sách flash item của sự kiện đó
     */
    public List<FlashSaleItem> findByEvent(String eventId) {
        return findBy(item -> item.getEventId().equalsIgnoreCase(eventId));
    }

    /**
     * Tìm tất cả flash item còn hàng ({@code soldQty < limitedQty}).
     *
     * @return danh sách flash item còn tồn kho
     */
    public List<FlashSaleItem> findAvailable() {
        return findBy(item -> item.soLuongConLai() > 0);
    }

    /**
     * Tìm tất cả flash item đã hết hàng ({@code soldQty >= limitedQty}).
     *
     * @return danh sách flash item đã bán hết
     */
    public List<FlashSaleItem> findSoldOut() {
        return findBy(item -> item.soLuongConLai() <= 0);
    }

    /**
     * Tìm flash item theo mã sản phẩm gốc.
     *
     * @param productId mã sản phẩm (e.g. {@code "PRD-00001"})
     * @return danh sách flash item của sản phẩm đó
     */
    public List<FlashSaleItem> findByProduct(String productId) {
        return findBy(item -> item.getProductId().equalsIgnoreCase(productId));
    }

    // -----------------------------------------------------------------------
    // MAINTENANCE
    // -----------------------------------------------------------------------

    /**
     * Reset tất cả {@code soldQty} về 0 và {@code version} về 1.
     * Dùng trước mỗi đợt simulation mới.
     */
    public void resetAllSoldQty() {
        List<FlashSaleItem> all = findAll();
        for (FlashSaleItem item : all) {
            item.setSoldQty(0);
            item.setVersion(1);
        }
        rewriteAll(all);
    }

    // =======================================================================
    // 4 CƠ CHẾ ĐỒNG BỘ — LOGIC TRỪ KHO
    // =======================================================================

    // -----------------------------------------------------------------------
    // 1. NO_LOCK — Baseline (không khóa → gây race condition, âm kho)
    // -----------------------------------------------------------------------

    /**
     * Bán hàng <b>KHÔNG CÓ KHÓA</b> — Baseline để so sánh.
     *
     * <p>
     * ⚠ Cơ chế này KHÔNG an toàn khi multi-thread:
     * <ul>
     * <li>Thread-1 đọc {@code soldQty=49}, Thread-2 đọc {@code soldQty=49}</li>
     * <li>Cả hai thấy còn hàng → cả hai ghi {@code soldQty=50}</li>
     * <li>Kết quả: mất 1 đơn hàng (hoặc âm kho nếu {@code limitedQty=49})</li>
     * </ul>
     *
     * @param flashItemId mã flash item cần bán
     * @param qty         số lượng muốn mua (phải {@code > 0})
     * @throws OutOfStockException     nếu không đủ hàng
     * @throws EntityNotFoundException nếu không tìm thấy flash item
     */
    public void sellNoLock(String flashItemId, int qty)
            throws OutOfStockException, EntityNotFoundException {
        FlashSaleItem item = findById(flashItemId)
                .orElseThrow(() -> new EntityNotFoundException("FlashSaleItem", flashItemId));

        if (!item.coBanDuoc(qty)) {
            throw new OutOfStockException(flashItemId, qty, item.soLuongConLai());
        }

        item.setSoldQty(item.getSoldQty() + qty);
        item.setVersion(item.getVersion() + 1);
        update(item);
    }

    // -----------------------------------------------------------------------
    // 2. SYNCHRONIZED — Đồng bộ hóa toàn bộ method
    // -----------------------------------------------------------------------

    /**
     * Bán hàng với {@code synchronized} — đảm bảo chỉ 1 thread thực thi tại 1 thời
     * điểm.
     *
     * <p>
     * An toàn tuyệt đối (0% âm kho) nhưng throughput thấp nhất vì
     * mọi thread phải xếp hàng tuần tự.
     *
     * @param flashItemId mã flash item cần bán
     * @param qty         số lượng muốn mua
     * @throws OutOfStockException     nếu không đủ hàng
     * @throws EntityNotFoundException nếu không tìm thấy flash item
     */
    public synchronized void sellWithSynchronized(String flashItemId, int qty)
            throws OutOfStockException, EntityNotFoundException {
        FlashSaleItem item = findById(flashItemId)
                .orElseThrow(() -> new EntityNotFoundException("FlashSaleItem", flashItemId));

        if (!item.coBanDuoc(qty)) {
            throw new OutOfStockException(flashItemId, qty, item.soLuongConLai());
        }

        item.setSoldQty(item.getSoldQty() + qty);
        item.setVersion(item.getVersion() + 1);
        update(item);
    }

    // -----------------------------------------------------------------------
    // 3. FILE_LOCK — Khóa file NIO (try-with-resources)
    // -----------------------------------------------------------------------

    /**
     * Bán hàng với {@code java.nio.channels.FileLock} — khóa file cấp hệ điều hành.
     *
     * <p>
     * Dùng {@code try-with-resources} cho {@link FileChannel} và {@link FileLock}:
     * <ul>
     * <li>Acquire exclusive lock trên toàn bộ file</li>
     * <li>Đọc → kiểm tra → cập nhật → ghi lại — tất cả trong khi giữ lock</li>
     * <li>Lock tự động release khi thoát try block</li>
     * </ul>
     *
     * <p>
     * An toàn cả cross-process (khác với {@code synchronized} chỉ trong cùng JVM).
     *
     * @param flashItemId mã flash item cần bán
     * @param qty         số lượng muốn mua
     * @throws OutOfStockException     nếu không đủ hàng
     * @throws EntityNotFoundException nếu không tìm thấy flash item
     */
    public void sellWithFileLock(String flashItemId, int qty)
            throws OutOfStockException, EntityNotFoundException {
        Path path = Paths.get(getFilePath());

        try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "rw");
                FileChannel channel = raf.getChannel();
                FileLock lock = channel.lock()) {

            // --- Đọc toàn bộ nội dung file trong khi giữ lock ---
            byte[] bytes = new byte[(int) channel.size()];
            ByteBuffer readBuffer = ByteBuffer.wrap(bytes);
            channel.read(readBuffer);
            String content = new String(bytes, StandardCharsets.UTF_8);

            // Strip BOM nếu có
            if (!content.isEmpty() && content.charAt(0) == '\uFEFF') {
                content = content.substring(1);
            }

            // --- Parse tất cả dòng ---
            String[] lines = content.split("\\r?\\n");
            String headerLine = lines.length > 0 ? lines[0] : getHeader();

            List<FlashSaleItem> items = new ArrayList<>();
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty())
                    continue;
                FlashSaleItem item = new FlashSaleItem();
                item.fromCsvLine(line);
                items.add(item);
            }

            // --- Tìm item cần bán ---
            FlashSaleItem target = null;
            for (FlashSaleItem item : items) {
                if (item.getFlashItemId().equals(flashItemId)) {
                    target = item;
                    break;
                }
            }
            if (target == null) {
                throw new EntityNotFoundException("FlashSaleItem", flashItemId);
            }

            // --- Kiểm tra tồn kho ---
            if (!target.coBanDuoc(qty)) {
                throw new OutOfStockException(flashItemId, qty, target.soLuongConLai());
            }

            // --- Cập nhật ---
            target.setSoldQty(target.getSoldQty() + qty);
            target.setVersion(target.getVersion() + 1);

            // --- Ghi lại toàn bộ file (vẫn đang giữ lock) ---
            channel.position(0);
            StringBuilder sb = new StringBuilder();
            sb.append('\uFEFF'); // BOM
            sb.append(headerLine);
            sb.append(System.lineSeparator());
            for (FlashSaleItem item : items) {
                sb.append(item.toCsvLine());
                sb.append(System.lineSeparator());
            }
            byte[] writeBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
            channel.truncate(writeBytes.length);
            channel.write(ByteBuffer.wrap(writeBytes));

        } catch (OutOfStockException | EntityNotFoundException e) {
            throw e; // Re-throw business exceptions
        } catch (IOException e) {
            System.err.println("[FlashSaleItemRepository] Lỗi FILE_LOCK: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // 4. OPTIMISTIC LOCK — Version field + retry loop (MAX_RETRY = 3)
    // -----------------------------------------------------------------------

    /**
     * Bán hàng với <b>Optimistic Lock</b> — kiểm tra {@code version} trước khi ghi.
     *
     * <p>
     * Chiến lược:
     * <ol>
     * <li><b>Đọc</b>: lấy entity, ghi nhớ {@code versionDoc}</li>
     * <li><b>Kiểm tra</b>: {@code coBanDuoc(qty)}?</li>
     * <li><b>Compare-and-Swap</b>: đọc lại file → nếu version vẫn bằng
     * {@code versionDoc} thì ghi bản cập nhật; nếu không → retry</li>
     * <li>Sau {@value #MAX_RETRY} lần thất bại → ném
     * {@link OptimisticLockException}</li>
     * </ol>
     *
     * <p>
     * Ưu điểm: throughput cao nhất vì không khóa toàn bộ — chỉ retry khi
     * thực sự có xung đột. Lý tưởng cho hệ thống có tỉ lệ xung đột thấp.
     *
     * @param flashItemId mã flash item cần bán
     * @param qty         số lượng muốn mua
     * @throws OutOfStockException     nếu không đủ hàng
     * @throws EntityNotFoundException nếu không tìm thấy flash item
     * @throws OptimisticLockException nếu xung đột version sau {@value #MAX_RETRY}
     *                                 lần retry
     */
    public void sellWithOptimisticLock(String flashItemId, int qty)
            throws OutOfStockException, EntityNotFoundException, OptimisticLockException {

        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            // Step 1: Đọc trạng thái hiện tại
            FlashSaleItem item = findById(flashItemId)
                    .orElseThrow(() -> new EntityNotFoundException("FlashSaleItem", flashItemId));

            int versionDoc = item.getVersion();

            // Step 2: Kiểm tra tồn kho (pre-check)
            if (!item.coBanDuoc(qty)) {
                throw new OutOfStockException(flashItemId, qty, item.soLuongConLai());
            }

            // Step 3: Chuẩn bị entity đã cập nhật
            FlashSaleItem updated = new FlashSaleItem(
                    item.getFlashItemId(), item.getEventId(), item.getProductId(),
                    item.getLimitedQty(), item.getSoldQty() + qty,
                    item.getFlashPrice(), versionDoc + 1);

            // Step 4: Compare-and-Swap (atomic read-check-write)
            boolean committed = false;
            synchronized (optimisticWriteLock) {
                List<FlashSaleItem> all = findAll();
                for (int i = 0; i < all.size(); i++) {
                    FlashSaleItem current = all.get(i);
                    if (current.getFlashItemId().equals(flashItemId)) {
                        if (current.getVersion() == versionDoc) {
                            // Version khớp — an toàn để ghi
                            all.set(i, updated);
                            rewriteAll(all);
                            committed = true;
                        }
                        // else: version đã thay đổi → retry
                        break;
                    }
                }
            }

            if (committed)
                return; // Thành công!

            // Version conflict — log và retry
            System.err.printf("[OPTIMISTIC] Xung đột version lần %d/%d cho item '%s'%n",
                    attempt, MAX_RETRY, flashItemId);
        }

        // Đã hết retry — ném exception
        FlashSaleItem current = findById(flashItemId).orElse(null);
        int currentVersion = (current != null) ? current.getVersion() : -1;
        throw new OptimisticLockException(flashItemId, -1, currentVersion);
    }
}
