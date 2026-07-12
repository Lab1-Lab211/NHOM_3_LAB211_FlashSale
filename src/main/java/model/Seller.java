package model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Tai khoan nguoi ban va danh sach tai nguyen thuoc quyen so huu. */
public class Seller extends BaseEntity {
    private String sellerId;
    private String name;
    private String email;
    private String passwordHash;
    private String passwordSalt;
    private List<String> productIds = new ArrayList<>();
    private List<String> eventIds = new ArrayList<>();
    private String registeredDate;

    public Seller() {
        super();
    }

    public Seller(String sellerId, String name, String email,
                  String passwordHash, String passwordSalt,
                  List<String> productIds, List<String> eventIds,
                  String registeredDate) {
        super(sellerId);
        this.sellerId = sellerId;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.passwordSalt = passwordSalt;
        if (productIds != null) this.productIds.addAll(productIds);
        if (eventIds != null) this.eventIds.addAll(eventIds);
        this.registeredDate = registeredDate;
    }

    @Override
    public String toCsvLine() {
        return String.join(",", sellerId, name, email, passwordHash, passwordSalt,
                String.join(";", productIds), String.join(";", eventIds), registeredDate);
    }

    @Override
    public void fromCsvLine(String csvLine) {
        String[] parts = csvLine.split(",", -1);
        if (parts.length < 8) {
            throw new IllegalArgumentException("Seller CSV phai co 8 cot");
        }
        sellerId = parts[0].trim();
        id = sellerId;
        name = parts[1].trim();
        email = parts[2].trim();
        passwordHash = parts[3].trim();
        passwordSalt = parts[4].trim();
        productIds = parseIds(parts[5]);
        eventIds = parseIds(parts[6]);
        registeredDate = parts[7].trim();
    }

    private List<String> parseIds(String value) {
        if (value == null || value.trim().isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(value.trim().split(";")));
    }

    @Override
    public String getCsvHeader() {
        return "sellerId,name,email,passwordHash,passwordSalt,productIds,eventIds,registeredDate";
    }

    public boolean ownsProduct(String productId) { return productIds.contains(productId); }
    public boolean ownsEvent(String eventId) { return eventIds.contains(eventId); }
    public void addProductId(String productId) { if (!ownsProduct(productId)) productIds.add(productId); }
    public void addEventId(String eventId) { if (!ownsEvent(eventId)) eventIds.add(eventId); }

    public String getSellerId() { return sellerId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getPasswordSalt() { return passwordSalt; }
    public List<String> getProductIds() { return Collections.unmodifiableList(productIds); }
    public List<String> getEventIds() { return Collections.unmodifiableList(eventIds); }
    public String getRegisteredDate() { return registeredDate; }
}
