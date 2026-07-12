package model.enums;

public enum UserRole {
    CUSTOMER("Nguoi mua"),
    SELLER("Nguoi ban"),
    ADMIN("Quan tri vien");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
