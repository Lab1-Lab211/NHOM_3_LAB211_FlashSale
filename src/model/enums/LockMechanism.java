package model.enums;

/**
 * Cơ chế đồng bộ hóa khi xử lý đơn hàng đồng thời.
 * Dùng để so sánh hiệu suất và độ an toàn trong Simulator.
 */
public enum LockMechanism {
    NO_LOCK("Không khóa (Baseline)"),
    FILE_LOCK("Khóa file (NIO FileLock)"),
    SYNCHRONIZED("Đồng bộ hóa (synchronized)"),
    OPTIMISTIC("Khóa lạc quan (Optimistic Lock)");

    private final String moTa;

    LockMechanism(String moTa) {
        this.moTa = moTa;
    }

    public String getMoTa() {
        return moTa;
    }
}
