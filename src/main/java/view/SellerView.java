package view;

import controller.SellerController;
import model.FlashSaleEvent;
import model.FlashSaleItem;
import model.Product;
import model.Seller;
import model.Order;
import model.OrderDetail;
import model.enums.OrderStatus;
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
                    case "5": showEventDetails(); break;
                    case "6": listEventItems(); break;
                    case "7": editFlashSale(); break;
                    case "8": startFlashSale(); break;
                    case "9": endFlashSale(); break;
                    case "10": listReceivedOrders(); break;
                    case "11": showReceivedOrderDetails(); break;
                    case "12": updateOrderStatus(); break;
                    case "13": sellerController.logout(); System.out.println("Da dang xuat nguoi ban."); break;
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
        System.out.println("3. Xem Flash Sale cua toi");
        System.out.println("4. Tao Flash Sale");
        System.out.println("5. Xem chi tiet mot Flash Sale");
        System.out.println("6. Xem hang hoa trong mot Flash Sale");
        System.out.println("7. Chinh sua Flash Sale (thong tin/them/xoa hang hoa)");
        System.out.println("8. Bat dau Flash Sale");
        System.out.println("9. Ket thuc Flash Sale");
        System.out.println("10. Xem danh sach don hang nhan duoc");
        System.out.println("11. Xem chi tiet don hang");
        System.out.println("12. Cap nhat trang thai don hang");
        System.out.println("13. Dang xuat");
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
            System.out.println("Da tao Flash Sale. Event ID: " + event.getEventId()
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
                    event.getEventId(), event.getEventName(), event.getEffectiveStatus().getMoTa(),
                    event.getStartTime(), event.getEndTime(), event.getDiscountPercent());

        }
    }

    private void addFlashItem(String eventId) {
        try {
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
            printEventItems(eventId);
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    private void showEventDetails() {
        try {
            String eventId = input.readLine("Event ID: ").trim();
            FlashSaleEvent event = sellerController.findOwnEvent(eventId)
                    .orElseThrow(() -> new IllegalArgumentException("Khong tim thay Flash Sale cua ban"));
            System.out.printf("%s | %s | %s | %s -> %s | giam %d%%%n",
                    event.getEventId(), event.getEventName(), event.getEffectiveStatus().getMoTa(),
                    event.getStartTime(), event.getEndTime(), event.getDiscountPercent());
            printEventItems(eventId);
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    private void editFlashSale() {
        String eventId = input.readLine("Event ID can chinh sua: ").trim();
        try {
            FlashSaleEvent event = sellerController.findOwnEvent(eventId)
                    .orElseThrow(() -> new IllegalArgumentException("Khong tim thay Flash Sale cua ban"));
            if (event.getStatus() != model.enums.SaleStatus.SAP_DIEN_RA) {
                throw new IllegalArgumentException("Chi duoc sua Flash Sale sap dien ra");
            }

            boolean editing = true;
            while (editing) {
                System.out.println("\n===== CHINH SUA " + eventId + " =====");
                System.out.println("1. Sua ten/thoi gian/phan tram giam");
                System.out.println("2. Them hang hoa");
                System.out.println("3. Xoa hang hoa");
                System.out.println("4. Xem hang hoa");
                System.out.println("0. Hoan tat chinh sua");
                String choice = input.readLine("Chon: ").trim();
                switch (choice) {
                    case "1": updateEventInfo(eventId); break;
                    case "2": addFlashItem(eventId); break;
                    case "3": removeFlashItem(eventId); break;
                    case "4": printEventItems(eventId); break;
                    case "0": editing = false; break;
                    default: System.out.println("Lua chon khong hop le.");
                }
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("Khong the chinh sua: " + e.getMessage());
        }
    }

    private void updateEventInfo(String eventId) {
        FlashSaleEvent current = sellerController.findOwnEvent(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay Flash Sale"));
        String name = input.readLine("Ten moi [Enter giu '" + current.getEventName() + "']: ");
        String start = input.readLine("Bat dau moi [Enter giu " + current.getStartTime() + "]: ");
        String end = input.readLine("Ket thuc moi [Enter giu " + current.getEndTime() + "]: ");
        String discountText = input.readLine("Phan tram giam moi [Enter giu "
                + current.getDiscountPercent() + "]: ").trim();
        Integer discount = null;
        if (!discountText.isEmpty()) {
            try { discount = Integer.valueOf(discountText); }
            catch (NumberFormatException e) { throw new IllegalArgumentException("Phan tram giam khong hop le"); }
        }
        FlashSaleEvent updated = sellerController.updateEvent(eventId, name, start, end, discount);
        System.out.println("Cap nhat thanh cong: " + updated.getEventName());
    }

    private void removeFlashItem(String eventId) {
        String flashItemId = input.readLine("Flash Item ID can xoa: ").trim();
        boolean removed = sellerController.removeItem(eventId, flashItemId);
        System.out.println(removed
                ? "Xoa hang hoa thanh cong; so luong chua ban da hoan lai kho."
                : "Xoa hang hoa that bai.");
    }

    private void startFlashSale() {
        try {
            String eventId = input.readLine("Event ID can bat dau: ").trim();
            FlashSaleEvent event = sellerController.startEvent(eventId);
            System.out.println("Flash Sale dang dien ra. Trang thai: " + event.getStatus().getMoTa());
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("Bat dau Flash Sale that bai: " + e.getMessage());
        }
    }

    private void endFlashSale() {
        try {
            String eventId = input.readLine("Event ID can ket thuc: ").trim();
            FlashSaleEvent event = sellerController.endEvent(eventId);
            System.out.println("Ket thuc Flash Sale thanh cong. Trang thai: " + event.getStatus().getMoTa());
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("Ket thuc Flash Sale that bai: " + e.getMessage());
        }
    }

    private void listReceivedOrders() {
        try {
            List<Order> orders = sellerController.getReceivedOrders();
            if (orders.isEmpty()) {
                System.out.println("Chua co don hang nao dat san pham cua ban.");
                return;
            }
            System.out.printf("%-12s %-12s %-12s %-20s %-22s %14s%n",
                    "Order ID", "Customer", "Event ID", "Thoi gian", "Trang thai", "Tong tien");
            for (Order order : orders) {
                System.out.printf("%-12s %-12s %-12s %-20s %-22s %14.0f%n",
                        order.getOrderId(), order.getCustomerId(), order.getEventId(),
                        order.getOrderTime(), order.getStatus().getMoTa(), order.getTotalAmount());
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("Khong the xem don hang: " + e.getMessage());
        }
    }

    private void showReceivedOrderDetails() {
        String orderId = input.readLine("Order ID: ").trim();
        try {
            Order order = sellerController.getReceivedOrder(orderId);
            List<OrderDetail> details = sellerController.getReceivedOrderDetails(orderId);
            System.out.printf("Don: %s | Khach hang: %s | Flash Sale: %s | %s | %s | Tong: %.0f%n",
                    order.getOrderId(), order.getCustomerId(), order.getEventId(),
                    order.getOrderTime(), order.getStatus().getMoTa(), order.getTotalAmount());
            if (details.isEmpty()) {
                System.out.println("Don hang khong co chi tiet.");
                return;
            }
            System.out.printf("%-12s %-12s %-12s %-30s %8s %12s %14s%n",
                    "Detail ID", "Item ref", "Product ID", "Ten san pham",
                    "So luong", "Don gia", "Thanh tien");
            for (OrderDetail detail : details) {
                String reference = detail.getFlashItemId();
                boolean regularProduct = reference != null && reference.startsWith("PRD-");
                FlashSaleItem item = regularProduct ? null
                        : sellerController.findFlashItemById(reference).orElse(null);
                String productId = regularProduct ? reference : (item == null ? "-" : item.getProductId());
                Product product = "-".equals(productId) ? null
                        : sellerController.findProductById(productId).orElse(null);
                String productName = product == null ? "Khong tim thay" : product.getName();
                System.out.printf("%-12s %-12s %-12s %-30s %8d %12.0f %14.0f%n",
                        detail.getDetailId(), detail.getFlashItemId(), productId, productName,
                        detail.getQuantity(), detail.getUnitPrice(), detail.thanhTien());
            }
        } catch (Exception e) {
            System.out.println("Khong the xem chi tiet don hang: " + e.getMessage());
        }
    }

    private void updateOrderStatus() {
        String orderId = input.readLine("Order ID can cap nhat: ").trim();
        System.out.println("1. Xac nhan don hang");
        System.out.println("2. Dang chuan bi hang");
        System.out.println("3. Dang giao hang");
        System.out.println("4. Tu choi don dang cho xac nhan");
        System.out.println("5. Giao hang that bai");
        int choice = input.readInt("Chon trang thai moi: ");
        OrderStatus status;
        switch (choice) {
            case 1: status = OrderStatus.DA_XAC_NHAN; break;
            case 2: status = OrderStatus.DANG_CHUAN_BI; break;
            case 3: status = OrderStatus.DANG_GIAO; break;
            case 4: status = OrderStatus.TU_CHOI; break;
            case 5: status = OrderStatus.GIAO_THAT_BAI; break;
            default:
                System.out.println("Trang thai khong hop le.");
                return;
        }
        try {
            Order updated = sellerController.updateOrderStatus(orderId, status);
            System.out.println("Cap nhat thanh cong. Trang thai moi: " + updated.getStatus().getMoTa());
        } catch (Exception e) {
            System.out.println("Cap nhat trang thai that bai: " + e.getMessage());
        }
    }

    private void printEventItems(String eventId) {
        List<FlashSaleItem> items = sellerController.getItemsByEvent(eventId);
        if (items.isEmpty()) {
            System.out.println("Flash Sale chua co hang hoa.");
            return;
        }
        System.out.printf("%-12s %-12s %-12s %-30s %-15s %8s %8s %8s %12s%n",
                "FlashItem", "Event ID", "Product ID", "Ten san pham", "Danh muc",
                "Gioi han", "Da ban", "Con lai", "Gia sale");
        for (FlashSaleItem item : items) {
            Product product = sellerController.findProductById(item.getProductId()).orElse(null);
            String productName = product == null ? "Khong tim thay" : product.getName();
            String category = product == null ? "-" : product.getCategory().getMoTa();
            System.out.printf("%-12s %-12s %-12s %-30s %-15s %8d %8d %8d %12.0f%n",
                    item.getFlashItemId(), item.getEventId(), item.getProductId(),
                    productName, category, item.getLimitedQty(), item.getSoldQty(),
                    item.soLuongConLai(), item.getFlashPrice());
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
