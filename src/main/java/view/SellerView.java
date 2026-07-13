package view;

import controller.SellerController;
import model.FlashSaleEvent;
import model.FlashSaleItem;
import model.Product;
import model.Seller;
import model.enums.ProductCategory;

import java.util.List;

public class SellerView {
    private final SellerController sellerController;
    private final ConsoleInput input;

    public SellerView(SellerController sellerController, ConsoleInput input) {
        this.sellerController = sellerController;
        this.input = input;
    }

    public void run() {
        boolean running = true;
        while (running) {
            if (!sellerController.isLoggedIn()) {
                System.out.println("\n===== CONG NGUOI BAN =====");
                System.out.println("1. Dang ky nguoi ban");
                System.out.println("2. Dang nhap nguoi ban");
                System.out.println("0. Quay lai chon role");
                String choice = input.readLine("Chon: ").trim();
                switch (choice) {
                    case "1": register(); break;
                    case "2": login(); break;
                    case "0": sellerController.logout(); running = false; break;
                    default: System.out.println("Lua chon khong hop le.");
                }
            } else {
                showSellerMenu();
                String choice = input.readLine("Chon: ").trim();
                switch (choice) {
                    case "1": listProducts(); break;
                    case "2": addProduct(); break;
                    case "3": listEvents(); break;
                    case "4": createFlashSale(); break;
                    case "5": addFlashItem(); break;
                    case "6": listEventItems(); break;
                    case "7": sellerController.logout(); System.out.println("Da dang xuat nguoi ban."); break;
                    case "0": sellerController.logout(); running = false; break;
                    default: System.out.println("Lua chon khong hop le.");
                }
            }
        }
    }

    private void showSellerMenu() {
        Seller seller = sellerController.getCurrentSeller();
        System.out.println("\n===== NGUOI BAN: " + seller.getSellerId() + " - " + seller.getName() + " =====");
        System.out.println("1. Xem san pham cua toi");
        System.out.println("2. Them san pham moi");
        System.out.println("3. Xem Flash Sale cua toi va trang thai phe duyet");
        System.out.println("4. Tao Flash Sale va gui Admin phe duyet");
        System.out.println("5. Them hang hoa vao Flash Sale cho duyet");
        System.out.println("6. Xem hang hoa trong mot Flash Sale");
        System.out.println("7. Dang xuat");
        System.out.println("0. Quay lai chon role");
    }

    private void register() {
        try {
            String name = input.readLine("Nhap ten nguoi ban: ");
            String email = input.readLine("Nhap email: ").trim();
            if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
                throw new IllegalArgumentException("Dinh dang email khong hop le");
            }
            String password = input.readPassword("Nhap mat khau (toi thieu 6 ky tu): ");
            String confirm = input.readPassword("Xac nhan mat khau: ");
            if (!password.equals(confirm)) throw new IllegalArgumentException("Mat khau xac nhan khong khop");
            Seller seller = sellerController.register(name, email, password);
            System.out.println("Dang ky thanh cong. Seller ID: " + seller.getSellerId());
        } catch (IllegalArgumentException e) {
            System.out.println("Dang ky that bai: " + e.getMessage());
        }
    }

    private void login() {
        String email = input.readLine("Nhap email: ").trim();
        String password = input.readPassword("Nhap mat khau: ");
        System.out.println(sellerController.login(email, password)
                ? "Dang nhap nguoi ban thanh cong." : "Email hoac mat khau khong dung.");
    }

    private void addProduct() {
        try {
            String name = input.readLine("Ten san pham: ");
            ProductCategory category = readCategory();
            double price = readDouble("Gia goc: ");
            int stock = input.readInt("So luong ton kho: ");
            Product product = sellerController.addProduct(name, category, price, stock);
            System.out.println("Them san pham thanh cong. Product ID: " + product.getProductId());
        } catch (IllegalArgumentException e) {
            System.out.println("Them san pham that bai: " + e.getMessage());
        }
    }

    private void listProducts() {
        List<Product> products = sellerController.getOwnProducts();
        if (products.isEmpty()) {
            System.out.println("Ban chua co san pham nao.");
            return;
        }
        printProducts(products);
    }

    private void createFlashSale() {
        try {
            String name = input.readLine("Ten Flash Sale: ");
            String start = input.readLine("Bat dau (yyyy-MM-dd'T'HH:mm:ss): ");
            String end = input.readLine("Ket thuc (yyyy-MM-dd'T'HH:mm:ss): ");
            int discount = input.readInt("Phan tram giam (1-99): ");
            FlashSaleEvent event = sellerController.createFlashSaleRequest(name, start, end, discount);
            System.out.println("Da tao va gui cho Admin. Event ID: " + event.getEventId()
                    + " | Trang thai: " + event.getStatus().getMoTa());
        } catch (IllegalArgumentException e) {
            System.out.println("Tao Flash Sale that bai: " + e.getMessage());
        }
    }

    private void listEvents() {
        List<FlashSaleEvent> events = sellerController.getOwnEvents();
        if (events.isEmpty()) {
            System.out.println("Ban chua tao Flash Sale nao.");
            return;
        }
        for (FlashSaleEvent event : events) {
            System.out.printf("%s | %s | %s | %s -> %s | giam %d%%%n",
                    event.getEventId(), event.getEventName(), event.getStatus().getMoTa(),
                    event.getStartTime(), event.getEndTime(), event.getDiscountPercent());
        }
    }

    private void addFlashItem() {
        try {
            String eventId = input.readLine("Event ID: ").trim();
            String productId = input.readLine("Product ID cua ban: ").trim();
            int qty = input.readInt("So luong dua vao Flash Sale: ");
            double price = readDouble("Gia Flash Sale: ");
            FlashSaleItem item = sellerController.addItem(eventId, productId, qty, price);
            System.out.println("Them hang hoa thanh cong. Flash Item ID: " + item.getFlashItemId());
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("Them hang hoa that bai: " + e.getMessage());
        }
    }

    private void listEventItems() {
        try {
            String eventId = input.readLine("Event ID: ").trim();
            List<FlashSaleItem> items = sellerController.getItemsByEvent(eventId);
            if (items.isEmpty()) {
                System.out.println("Flash Sale chua co hang hoa.");
                return;
            }
            for (FlashSaleItem item : items) System.out.println(item);
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    private ProductCategory readCategory() {
        ProductCategory[] values = ProductCategory.values();
        for (int i = 0; i < values.length; i++) {
            System.out.printf("%d. %s%n", i + 1, values[i].getMoTa());
        }
        int choice = input.readInt("Chon danh muc: ");
        if (choice < 1 || choice > values.length) throw new IllegalArgumentException("Danh muc khong hop le");
        return values[choice - 1];
    }

    private double readDouble(String prompt) {
        try { return Double.parseDouble(input.readLine(prompt).trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Gia tri so khong hop le"); }
    }

    private void printProducts(List<Product> products) {
        System.out.printf("%-12s %-30s %-15s %12s %8s%n", "Product ID", "Ten", "Danh muc", "Gia", "Ton kho");
        for (Product p : products) {
            System.out.printf("%-12s %-30s %-15s %12.0f %8d%n",
                    p.getProductId(), p.getName(), p.getCategory().getMoTa(), p.getOriginalPrice(), p.getStock());
        }
    }
}
