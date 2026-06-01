package exception;

/**
 * Exception cơ sở cho toàn bộ hệ thống Flash Sale.
 * Tất cả custom exception khác đều kế thừa từ đây.
 */
public class FlashSaleException extends Exception {

    public FlashSaleException(String message) {
        super(message);
    }

    public FlashSaleException(String message, Throwable cause) {
        super(message, cause);
    }
}
