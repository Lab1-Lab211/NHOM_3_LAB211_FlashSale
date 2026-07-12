package view;

import controller.CustomerController;
import controller.FlashSaleController;
import controller.OrderController;
import controller.ProductController;
import controller.SellerController;
import controller.SimulatorController;
import model.Customer;
import model.Product;
import model.enums.ProductCategory;
import repository.CustomerRepository;
import repository.FlashSaleEventRepository;
import repository.FlashSaleItemRepository;
import repository.OrderDetailRepository;
import repository.OrderRepository;
import repository.OrderTransactionRepository;
import repository.ProductRepository;
import repository.SellerRepository;
import service.BookingResult;
import service.CustomerService;
import service.FlashSaleItemService;
import service.FlashSaleService;
import service.OrderService;
import service.ProductService;
import service.SellerService;
import service.SimulatorService;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

public class MainView {
    private final ConsoleInput input = new ConsoleInput();
    private final CustomerController customerController;
    private final FlashSaleView flashSaleView;
    private final OrderView orderView;
    private final ReportView reportView;
    private final SimulatorView simulatorView;
    private final ProductController productController;
    private final SellerView sellerView;
    private final AdminView adminView;
    private BookingResult lastBookingResult;

    public MainView() {
        Path dataRoot = resolveDataRoot();
        Path customerCsv = dataRoot.resolve("customers.csv");
        Path eventCsv = dataRoot.resolve("flash_events.csv");
        Path itemCsv = dataRoot.resolve("flash_items.csv");
        Path productCsv = dataRoot.resolve("products.csv");
        Path orderCsv = dataRoot.resolve("orders.csv");
        Path orderDetailCsv = dataRoot.resolve("order_details.csv");
        Path transactionCsv = dataRoot.resolve("transactions.csv");
        Path sellerCsv = dataRoot.resolve("sellers.csv");

        if (!Files.exists(eventCsv) || !Files.exists(itemCsv)) {
            System.err.println("Khong tim thay file data flash sale. Duong dan dang su dung: " + dataRoot);
        }

        CustomerRepository customerRepository = new CustomerRepository(customerCsv.toString());
        FlashSaleEventRepository eventRepository = new FlashSaleEventRepository(eventCsv.toString());
        FlashSaleItemRepository itemRepository = new FlashSaleItemRepository(itemCsv.toString());
        ProductRepository productRepository = new ProductRepository(productCsv.toString());
        OrderRepository orderRepository = new OrderRepository(orderCsv.toString());
        OrderDetailRepository orderDetailRepository = new OrderDetailRepository(orderDetailCsv.toString());
        OrderTransactionRepository transactionRepository = new OrderTransactionRepository(transactionCsv.toString());
        SellerRepository sellerRepository = new SellerRepository(sellerCsv.toString());

        CustomerService customerService = new CustomerService(customerRepository);
        FlashSaleItemService itemService = new FlashSaleItemService(itemRepository, eventRepository, productRepository);
        FlashSaleService flashSaleService = new FlashSaleService(eventRepository, itemService);
        OrderService orderService = new OrderService(
                orderRepository, orderDetailRepository, itemRepository, eventRepository, customerRepository);
        SimulatorService simulatorService = new SimulatorService(itemRepository, transactionRepository);
        ProductService productService = new ProductService(productRepository);
        SellerService sellerService = new SellerService(sellerRepository, productRepository,
                eventRepository, itemRepository, itemService);

        this.customerController = new CustomerController(customerService);
        FlashSaleController flashSaleController = new FlashSaleController(flashSaleService);
        OrderController orderController = new OrderController(orderService);
        SimulatorController simulatorController = new SimulatorController(simulatorService);
        SellerController sellerController = new SellerController(sellerService);

        this.flashSaleView = new FlashSaleView(flashSaleController);
        this.orderView = new OrderView(orderController, customerController, input);
        this.reportView = new ReportView();
        this.simulatorView = new SimulatorView(simulatorController, input);
        this.productController = new ProductController(productService);
        this.sellerView = new SellerView(sellerController, input);
        this.adminView = new AdminView(flashSaleController, simulatorView, input);
    }

    public static void main(String[] args) {
        configureUtf8Console();
        new MainView().run();
    }

    public void run() {
        boolean running = true;
        while (running) {
            showRoleMenu();
            String choice = input.readLine("Chon role: ").trim();
            switch (choice) {
                case "1": runBuyerPortal(); break;
                case "2": sellerView.run(); break;
                case "3": adminView.loginAndRun(); break;
                case "0": running = false; break;
                default: System.out.println("Lua chon khong hop le.");
            }
        }
        System.out.println("Tam biet.");
    }

    private void showRoleMenu() {
        System.out.println("\n===== HE THONG FLASH SALE =====");
        System.out.println("1. Nguoi mua");
        System.out.println("2. Nguoi ban");
        System.out.println("3. Admin");
        System.out.println("0. Thoat");
    }

    private void runBuyerPortal() {
        boolean running = true;
        while (running) {
            showBuyerMenu();
            String choice = input.readLine("Chon: ").trim();
            try {
                switch (choice) {
                    case "1": register(); break;
                    case "2": login(); break;
                    case "3": flashSaleView.showActiveItems(); break;
                    case "4": searchProductByName(); break;
                    case "5": filterProductByCategory(); break;
                    case "6": filterProductByPrice(); break;
                    case "7":
                        lastBookingResult = orderView.placeOrderSafely();
                        reportView.showBookingResult(lastBookingResult);
                        break;
                    case "8": orderView.showMyOrders(); break;
                    case "9": orderView.cancelMyOrder(); break;
                    case "10": reportView.showBookingResult(lastBookingResult); break;
                    case "11": customerController.logout(); System.out.println("Da dang xuat nguoi mua."); break;
                    case "0": customerController.logout(); running = false; break;
                    default: System.out.println("Lua chon khong hop le.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("Thao tac that bai: " + e.getMessage());
            }
        }
    }

    private void showBuyerMenu() {
        System.out.println();
        System.out.println("===== CONG NGUOI MUA =====");
        if (customerController.isLoggedIn()) {
            System.out.println("Dang login: " + customerController.getCurrentCustomer().getCustomerId()
                    + " - " + customerController.getCurrentCustomer().getName()
                    + " | Role: " + customerController.getCurrentRole().getDisplayName());
        } else {
            System.out.println("Chua login.");
        }
        System.out.println("1. Dang ky nguoi mua");
        System.out.println("2. Dang nhap nguoi mua");
        System.out.println("3. Xem san pham dang sale");
        System.out.println("4. Tim kiem san pham theo ten");
        System.out.println("5. Loc san pham theo danh muc");
        System.out.println("6. Loc san pham theo khoang gia");
        System.out.println("7. Dat hang");
        System.out.println("8. Xem trang thai/lich su don hang");
        System.out.println("9. Huy don hang");
        System.out.println("10. Xem ket qua dat hang gan nhat");
        System.out.println("11. Dang xuat");
        System.out.println("0. Quay lai chon role");
    }

    private void searchProductByName() {
        String keyword = input.readLine("Nhap ten san pham can tim: ");
        printProducts(productController.searchByName(keyword));
    }

    private void filterProductByCategory() {
        ProductCategory[] categories = ProductCategory.values();
        for (int i = 0; i < categories.length; i++) {
            System.out.printf("%d. %s%n", i + 1, categories[i].getMoTa());
        }
        int choice = input.readInt("Chon danh muc: ");
        if (choice < 1 || choice > categories.length) {
            throw new IllegalArgumentException("Danh muc khong hop le");
        }
        printProducts(productController.filterByCategory(categories[choice - 1]));
    }

    private void filterProductByPrice() {
        double min = readDouble("Gia toi thieu: ");
        double max = readDouble("Gia toi da: ");
        printProducts(productController.filterByPrice(min, max));
    }

    private double readDouble(String prompt) {
        try { return Double.parseDouble(input.readLine(prompt).trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Gia khong hop le"); }
    }

    private void printProducts(List<Product> products) {
        if (products.isEmpty()) {
            System.out.println("Khong tim thay san pham phu hop.");
            return;
        }
        System.out.printf("%-12s %-30s %-15s %12s %8s%n",
                "Product ID", "Ten", "Danh muc", "Gia goc", "Ton kho");
        for (Product p : products) {
            System.out.printf("%-12s %-30s %-15s %12.0f %8d%n",
                    p.getProductId(), p.getName(), p.getCategory().getMoTa(),
                    p.getOriginalPrice(), p.getStock());
        }
    }

    private void register() {
        String name = input.readLine("Nhap ten: ").trim();
        String email = input.readLine("Nhap email: ").trim();
        String password = input.readPassword("Nhap mat khau (toi thieu 6 ky tu): ");
        String confirmPassword = input.readPassword("Xac nhan mat khau: ");

        try {
            if (!password.equals(confirmPassword)) {
                throw new IllegalArgumentException("Xac nhan mat khau khong khop");
            }
            Customer customer = customerController.register(name, email, password);
            System.out.println("Register thanh cong. Customer ID: " + customer.getCustomerId()
                    + " | Tier mac dinh: " + customer.getTier());
        } catch (IllegalArgumentException e) {
            System.out.println("Register that bai: " + e.getMessage());
        }
    }

    private void login() {
        String email = input.readLine("Nhap email: ").trim();
        String password = input.readPassword("Nhap mat khau: ");
        Optional<Customer> customer = customerController.login(email, password);
        if (customer.isPresent()) {
            System.out.println("Login thanh cong. Xin chao " + customer.get().getName());
        } else {
            System.out.println("Email hoac mat khau khong dung.");
        }
    }

    private Path resolveDataRoot() {
        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("pom.xml"))) {
                return current.resolve("data");
            }
            current = current.getParent();
        }
        return Paths.get("data").toAbsolutePath();
    }

    private static void configureUtf8Console() {
        enableWindowsUtf8CodePage();
        try {
            System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8.name()));
            System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8.name()));
        } catch (UnsupportedEncodingException e) {
            System.err.println("Khong the cau hinh console UTF-8: " + e.getMessage());
        }
    }

    private static void enableWindowsUtf8CodePage() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (!osName.contains("win")) {
            return;
        }
        try {
            new ProcessBuilder("cmd", "/c", "chcp 65001 > nul")
                    .inheritIO()
                    .start()
                    .waitFor();
        } catch (Exception ignored) {
            // Neu khong doi duoc code page, van fallback bang Console/UTF-8 scanner.
        }
    }
}
