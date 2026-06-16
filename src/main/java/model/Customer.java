package model;

import model.enums.CustomerTier;

/**
 * Thực thể Khách hàng — đại diện cho người mua trong hệ thống Flash Sale.
 * File CSV tương ứng: customers.csv
 */
public class Customer extends BaseEntity {

    private String customerId;
    private String name;              // Họ và tên
    private String email;             // Email
    private CustomerTier tier;        // Hạng thành viên (VIP/PREMIUM/REGULAR)
    private String registeredDate;    // Ngày đăng ký (yyyy-MM-dd)

    /** Constructor mặc định */
    public Customer() {
        super();
    }

    /** Constructor đầy đủ tham số */
    public Customer(String customerId, String name, String email,
                    CustomerTier tier, String registeredDate) {
        super(customerId);
        this.customerId = customerId;
        this.name = name;
        this.email = email;
        this.tier = tier;
        this.registeredDate = registeredDate;
    }

    @Override
    public String toCsvLine() {
        return String.join(",",
                customerId, name, email, tier.name(), registeredDate
        );
    }

    @Override
    public void fromCsvLine(String csvLine) {
        String[] parts = csvLine.split(",", -1);
        this.customerId = parts[0].trim();
        this.id = this.customerId;
        this.name = parts[1].trim();
        this.email = parts[2].trim();
        this.tier = CustomerTier.valueOf(parts[3].trim());
        this.registeredDate = parts[4].trim();
    }

    @Override
    public String getCsvHeader() {
        return "customerId,name,email,tier,registeredDate";
    }

    // === Getter & Setter ===

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; this.id = customerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public CustomerTier getTier() { return tier; }
    public void setTier(CustomerTier tier) { this.tier = tier; }

    public String getRegisteredDate() { return registeredDate; }
    public void setRegisteredDate(String registeredDate) { this.registeredDate = registeredDate; }

    @Override
    public String toString() {
        return String.format("Customer{id='%s', ten='%s', email='%s', hang=%s, ngayDK='%s'}",
                customerId, name, email, tier.getMoTa(), registeredDate);
    }
}
