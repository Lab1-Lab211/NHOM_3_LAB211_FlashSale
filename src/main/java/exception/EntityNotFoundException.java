package exception;

/**
 * Exception khi không tìm thấy entity với ID chỉ định trong file CSV.
 * Ném khi tìm kiếm Product, Customer, FlashSaleItem, v.v. theo ID mà không có kết quả.
 */
public class EntityNotFoundException extends FlashSaleException {

    private final String entityType;
    private final String entityId;

    public EntityNotFoundException(String entityType, String entityId) {
        super(String.format("Không tìm thấy %s với ID '%s'.", entityType, entityId));
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
}
