package view;

import controller.CustomerController;
import controller.FlashSaleController;
import controller.OrderController;
import model.Customer;
import repository.CustomerRepository;
import repository.FlashSaleEventRepository;
import repository.FlashSaleItemRepository;
import repository.OrderDetailRepository;
import repository.OrderRepository;
import repository.ProductRepository;
import service.BookingResult;
import service.CustomerService;
import service.FlashSaleItemService;
import service.FlashSaleService;
import service.OrderService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Scanner;

public class MainView {
    private final Scanner scanner;
    private final CustomerController customerController;
    private final FlashSaleView flashSaleView;
    private final AdminFlashSaleView adminFlashSaleView;
    private final OrderView orderView;
    private final ReportView reportView;
    private BookingResult lastBookingResult;

    public MainView() throws Exception {
        this.scanner = new Scanner(System.in, "UTF-8");
        Path dataRoot = resolveDataRoot();
        Path customerCsv = dataRoot.resolve("customers.csv");
        Path eventCsv = dataRoot.resolve("flash_events.csv");
        Path itemCsv = dataRoot.resolve("flash_items.csv");
        Path orderCsv = dataRoot.resolve("orders.csv");
        Path orderDetailCsv = dataRoot.resolve("order_details.csv");
        Path productCsv = dataRoot.resolve("products.csv");

        if (!Files.exists(eventCsv) || !Files.exists(itemCsv)) {
            System.err.println("Không tìm thấy file data flash sale. Đường dẫn đang sử dụng: " + dataRoot);
        }

        CustomerRepository customerRepository = new CustomerRepository(customerCsv.toString());
        FlashSaleEventRepository eventRepository = new FlashSaleEventRepository(eventCsv.toString());
        FlashSaleItemRepository itemRepository = new FlashSaleItemRepository(itemCsv.toString());
        OrderRepository orderRepository = new OrderRepository(orderCsv.toString());
        OrderDetailRepository orderDetailRepository = new OrderDetailRepository(orderDetailCsv.toString());
        ProductRepository productRepository = new ProductRepository(productCsv.toString());

        CustomerService customerService = new CustomerService(customerRepository);
        FlashSaleItemService itemService = new FlashSaleItemService(itemRepository, eventRepository, productRepository);
        FlashSaleService flashSaleService = new FlashSaleService(eventRepository, itemService);
        OrderService orderService = new OrderService(
                orderRepository, orderDetailRepository, itemRepository, eventRepository, customerRepository);

        this.customerController = new CustomerController(customerService);
        FlashSaleController flashSaleController = new FlashSaleController(flashSaleService);
        OrderController orderController = new OrderController(orderService);

        this.flashSaleView = new FlashSaleView(flashSaleController);
        this.adminFlashSaleView = new AdminFlashSaleView(flashSaleController, scanner);
        this.orderView = new OrderView(orderController, customerController, scanner);
        this.reportView = new ReportView();
    }

    public static void main(String[] args) {
        try {
            System.setOut(new java.io.PrintStream(System.out, true, "UTF-8"));
            MainView view = new MainView();
            view.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        boolean running = true;
        while (running) {
            showMenu();
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    register();
                    break;
                case "2":
                    login();
                    break;
                case "3":
                    flashSaleView.showActiveItems();
                    break;
                case "4":
                    lastBookingResult = orderView.placeOrderNoLock();
                    reportView.showBookingResult(lastBookingResult);
                    break;
                case "5":
                    reportView.showBookingResult(lastBookingResult);
                    break;
                case "6":
                    customerController.logout();
                    System.out.println("Da logout.");
                    break;
                case "7":
                    System.out.print("Nhập mật khẩu Admin: ");
                    String password = scanner.nextLine().trim();
                    if ("admin123".equals(password)) {
                        adminFlashSaleView.showAdminMenu();
                    } else {
                        System.out.println("Sai mật khẩu! Không thể truy cập Admin.");
                    }
                    break;
                case "0":
                    running = false;
                    break;
                default:
                    System.out.println("Lựa chọn không hợp lệ.");
                    break;
            }
        }
        System.out.println("Tạm biệt.");
    }

    private void showMenu() {
        System.out.println();
        System.out.println("===== FLASH SALE SIMULATOR =====");
        if (customerController.isLoggedIn()) {
            System.out.println("Đang login: " + customerController.getCurrentCustomer().getCustomerId()
                    + " - " + customerController.getCurrentCustomer().getName());
        } else {
            System.out.println("Chưa login.");
        }
        System.out.println("1. Đăng ký khách hàng");
        System.out.println("2. Đăng nhập khách hàng");
        System.out.println("3. Xem sản phẩm đang sale");
        System.out.println("4. Đặt hàng NO_LOCK");
        System.out.println("5. Xem kết quả/report");
        System.out.println("6. Đăng xuất");
        System.out.println("7. Admin - Quản lý Flash Sale");
        System.out.println("0. Thoát");
        System.out.print("Chọn: ");
    }

    private void register() {
        System.out.print("Nhap ten: ");
        String name = scanner.nextLine().trim();
        System.out.print("Nhap email: ");
        String email = scanner.nextLine().trim();

        try {
            Customer customer = customerController.register(name, email);
            System.out.println("Register thanh cong. Customer ID: " + customer.getCustomerId()
                    + " | Tier mac dinh: " + customer.getTier());
        } catch (IllegalArgumentException e) {
            System.out.println("Register that bai: " + e.getMessage());
        }
    }

    private void login() {
        System.out.print("Nhap email: ");
        String email = scanner.nextLine().trim();
        Optional<Customer> customer = customerController.login(email);
        if (customer.isPresent()) {
            System.out.println("Login thanh cong. Xin chao " + customer.get().getName());
        } else {
            System.out.println("Khong tim thay customer voi email nay.");
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
}
