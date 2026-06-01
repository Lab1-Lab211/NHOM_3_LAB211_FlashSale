package exception;

/**
 * Exception khi xảy ra xung đột version trong Optimistic Lock.
 * Ném khi thread đọc version A nhưng lúc ghi thì version đã là B (thread khác đã ghi trước).
 */
public class OptimisticLockException extends FlashSaleException {

    private final String entityId;
    private final int versionDoc;      // Version đọc được lúc đầu
    private final int versionHienTai;  // Version hiện tại trong file (đã bị thay đổi)

    public OptimisticLockException(String entityId, int versionDoc, int versionHienTai) {
        super(String.format("Xung đột Optimistic Lock! Entity '%s': version đọc=%d, version hiện tại=%d. "
                + "Thread khác đã cập nhật trước — cần retry.", entityId, versionDoc, versionHienTai));
        this.entityId = entityId;
        this.versionDoc = versionDoc;
        this.versionHienTai = versionHienTai;
    }

    public String getEntityId() { return entityId; }
    public int getVersionDoc() { return versionDoc; }
    public int getVersionHienTai() { return versionHienTai; }
}
