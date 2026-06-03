package repository;

import model.BaseEntity;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Generic base repository cho việc đọc/ghi entity từ/vào file CSV.
 *
 * <p>Sử dụng trực tiếp các method từ {@link BaseEntity}:
 * <ul>
 *   <li>{@link BaseEntity#toCsvLine()} — serialize entity thành dòng CSV</li>
 *   <li>{@link BaseEntity#fromCsvLine(String)} — parse dòng CSV thành entity</li>
 *   <li>{@link BaseEntity#getCsvHeader()} — trả về header CSV</li>
 *   <li>{@link BaseEntity#getId()} — trả về ID duy nhất</li>
 * </ul>
 *
 * <p>Subclass chỉ cần cung cấp {@link Supplier}{@code <T>} (entity factory)
 * cho constructor — không cần implement abstract method nào.
 *
 * <p>Hỗ trợ UTF-8 BOM (tương thích {@code DataGenerator}),
 * try-with-resources cho mọi I/O, CRUD đầy đủ.
 *
 * @param <T> kiểu entity kế thừa {@link BaseEntity}
 */
public class CsvRepository<T extends BaseEntity> {

    /** Ký tự BOM (Byte Order Mark) cho UTF-8 — Excel đọc đúng tiếng Việt. */
    private static final char BOM = '\uFEFF';

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    /** Đường dẫn tới file CSV. */
    private final String filePath;

    /** Factory tạo instance mới của T — dùng cho parse CSV. */
    private final Supplier<T> entityFactory;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Khởi tạo repository.
     * Nếu file chưa tồn tại hoặc rỗng, tự động tạo file và ghi header.
     *
     * @param filePath      đường dẫn tuyệt đối hoặc tương đối tới file CSV
     * @param entityFactory {@link Supplier} tạo instance mới (e.g. {@code Product::new})
     */
    protected CsvRepository(String filePath, Supplier<T> entityFactory) {
        this.filePath = filePath;
        this.entityFactory = entityFactory;
        ensureFileWithHeader();
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    /** Trả về đường dẫn file CSV. */
    public String getFilePath() {
        return filePath;
    }

    /** Trả về entity factory. */
    protected Supplier<T> getEntityFactory() {
        return entityFactory;
    }

    /** Trả về dòng header CSV (lấy từ entity factory). */
    protected String getHeader() {
        return entityFactory.get().getCsvHeader();
    }

    // -----------------------------------------------------------------------
    // CREATE
    // -----------------------------------------------------------------------

    /**
     * Ghi thêm một entity vào cuối file CSV (append mode).
     *
     * @param entity entity cần lưu — không được {@code null}
     * @throws IllegalArgumentException nếu entity là {@code null}
     */
    public void save(T entity) {
        if (entity == null) throw new IllegalArgumentException("Entity không được null");
        try (BufferedWriter bw = Files.newBufferedWriter(
                Paths.get(filePath), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            bw.write(entity.toCsvLine());
            bw.newLine();
        } catch (IOException e) {
            System.err.println("[CsvRepository] Lỗi khi lưu: " + e.getMessage());
        }
    }

    /**
     * Ghi thêm nhiều entity vào cuối file CSV (batch append).
     *
     * @param entities danh sách entity cần lưu
     */
    public void saveAll(List<T> entities) {
        if (entities == null || entities.isEmpty()) return;
        try (BufferedWriter bw = Files.newBufferedWriter(
                Paths.get(filePath), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            for (T entity : entities) {
                bw.write(entity.toCsvLine());
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("[CsvRepository] Lỗi khi lưu batch: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // READ
    // -----------------------------------------------------------------------

    /**
     * Đọc toàn bộ entity từ file CSV.
     * Bỏ qua dòng header (dòng đầu tiên) và các dòng rỗng/lỗi.
     *
     * @return danh sách entity, không bao giờ {@code null}
     */
    public List<T> findAll() {
        List<T> result = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);
            for (int i = 1; i < lines.size(); i++) { // skip line 0 = header (có thể chứa BOM)
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;

                try {
                    T entity = entityFactory.get();
                    entity.fromCsvLine(line);
                    result.add(entity);
                } catch (Exception e) {
                    System.err.println("[CsvRepository] Bỏ qua dòng " + (i + 1)
                            + " trong " + filePath + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("[CsvRepository] Lỗi khi đọc " + filePath + ": " + e.getMessage());
        }
        return result;
    }

    /**
     * Tìm entity theo ID (sử dụng {@link BaseEntity#getId()}).
     *
     * @param id ID cần tìm
     * @return {@link Optional} chứa entity nếu tìm thấy
     */
    public Optional<T> findById(String id) {
        return findAll().stream()
                .filter(e -> e.getId() != null && e.getId().equals(id))
                .findFirst();
    }

    /**
     * Lọc entity theo tiêu chí bất kỳ (functional style).
     *
     * <pre>{@code
     * repo.findBy(p -> p.getCategory() == ProductCategory.DIEN_TU);
     * repo.findBy(c -> c.getTier() == CustomerTier.VIP);
     * }</pre>
     *
     * @param filter {@link Predicate} xác định điều kiện lọc
     * @return danh sách entity thỏa điều kiện, không bao giờ {@code null}
     */
    public List<T> findBy(Predicate<T> filter) {
        return findAll().stream()
                .filter(filter)
                .collect(Collectors.toList());
    }

    /**
     * Đếm số lượng entity trong file (không tính header).
     *
     * @return số lượng entity
     */
    public long count() {
        return findAll().size();
    }

    // -----------------------------------------------------------------------
    // UPDATE
    // -----------------------------------------------------------------------

    /**
     * Cập nhật một entity (tìm theo ID, thay thế, ghi lại toàn bộ file).
     *
     * @param updated entity đã cập nhật (ID phải khớp với record hiện có)
     */
    public void update(T updated) {
        List<T> all = findAll();
        String targetId = updated.getId();
        boolean found = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(targetId)) {
                all.set(i, updated);
                found = true;
                break;
            }
        }
        if (found) {
            rewriteAll(all);
        }
    }

    // -----------------------------------------------------------------------
    // DELETE
    // -----------------------------------------------------------------------

    /**
     * Xóa một entity theo ID, ghi lại toàn bộ file.
     *
     * @param id ID của entity cần xóa
     * @return {@code true} nếu xóa thành công, {@code false} nếu không tìm thấy
     */
    public boolean deleteById(String id) {
        List<T> all = findAll();
        List<T> remaining = all.stream()
                .filter(e -> !e.getId().equals(id))
                .collect(Collectors.toList());
        if (remaining.size() == all.size()) return false;
        rewriteAll(remaining);
        return true;
    }

    // -----------------------------------------------------------------------
    // HELPERS (protected — subclass có thể gọi/override)
    // -----------------------------------------------------------------------

    /**
     * Ghi lại toàn bộ file với danh sách entity mới (BOM + header + data).
     * Dùng khi cần cập nhật/xóa record.
     *
     * @param entities danh sách entity mới
     */
    protected void rewriteAll(List<T> entities) {
        try (BufferedWriter bw = Files.newBufferedWriter(
                Paths.get(filePath), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            bw.write(BOM);
            bw.write(getHeader());
            bw.newLine();
            for (T e : entities) {
                bw.write(e.toCsvLine());
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("[CsvRepository] Lỗi khi ghi lại file: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // PRIVATE
    // -----------------------------------------------------------------------

    /**
     * Tạo file CSV với BOM + header nếu file chưa tồn tại hoặc rỗng.
     */
    private void ensureFileWithHeader() {
        File f = new File(filePath);
        // Tạo thư mục cha nếu chưa có
        if (f.getParentFile() != null) {
            f.getParentFile().mkdirs();
        }
        if (!f.exists() || f.length() == 0) {
            try (BufferedWriter bw = Files.newBufferedWriter(
                    Paths.get(filePath), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE)) {
                bw.write(BOM);
                bw.write(getHeader());
                bw.newLine();
            } catch (IOException e) {
                System.err.println("[CsvRepository] Lỗi khi khởi tạo file: " + e.getMessage());
            }
        }
    }
}
