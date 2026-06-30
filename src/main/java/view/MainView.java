package view;

import controller.CustomerController;
import controller.FlashSaleController;
import controller.OrderController;
import model.Customer;
import model.enums.CustomerTier;
import repository.CustomerRepository;
import repository.FlashSaleEventRepository;
import repository.FlashSaleItemRepository;
import repository.OrderDetailRepository;
import repository.OrderRepository;
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
    private final Scanner scanner = new Scanner(System.in);
    private final CustomerController customerController;
    private final FlashSaleView flashSaleView;
    private final OrderView orderView;
    private final ReportView reportView;
    private BookingResult lastBookingResult;

    public MainView() {
        Path dataRoot = resolveDataRoot();
        Path customerCsv = dataRoot.resolve("customers.csv");
        Path eventCsv = dataRoot.resolve("flash_events.csv");
        Path itemCsv = dataRoot.resolve("flash_items.csv");
        Path orderCsv = dataRoot.resolve("orders.csv");
        Path orderDetailCsv = dataRoot.resolve("order_details.csv");

        if (!Files.exists(eventCsv) || !Files.exists(itemCsv)) {
            System.err.println("Khong tim thay file data flash sale. Duong dan dang su dung: " + dataRoot);
        }

        CustomerRepository customerRepository = new CustomerRepository(customerCsv.toString());
        FlashSaleEventRepository eventRepository = new FlashSaleEventRepository(eventCsv.toString());
        FlashSaleItemRepository itemRepository = new FlashSaleItemRepository(itemCsv.toString());
        OrderRepository orderRepository = new OrderRepository(orderCsv.toString());
        OrderDetailRepository orderDetailRepository = new OrderDetailRepository(orderDetailCsv.toString());

        CustomerService customerService = new CustomerService(customerRepository);
        FlashSaleItemService itemService = new FlashSaleItemService(itemRepository, eventRepository);
        FlashSaleService flashSaleService = new FlashSaleService(eventRepository, itemService);
        OrderService orderService = new OrderService(
                orderRepository, orderDetailRepository, itemRepository, eventRepository);

        this.customerController = new CustomerController(customerService);
        FlashSaleController flashSaleController = new FlashSaleController(flashSaleService);
        OrderController orderController = new OrderController(orderService);

        this.flashSaleView = new FlashSaleView(flashSaleController);
        this.orderView = new OrderView(orderController, customerController, scanner);
        this.reportView = new ReportView();
    }

    public static void main(String[] args) {
        new MainView().run();
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
                case "0":
                    running = false;
                    break;
                default:
                    System.out.println("Lua chon khong hop le.");
                    break;
            }
        }
        System.out.println("Tam biet.");
    }

    private void showMenu() {
        System.out.println();
        System.out.println("===== FLASH SALE SIMULATOR =====");
        if (customerController.isLoggedIn()) {
            System.out.println("Dang login: " + customerController.getCurrentCustomer().getCustomerId()
                    + " - " + customerController.getCurrentCustomer().getName());
        } else {
            System.out.println("Chua login.");
        }
        System.out.println("1. Register customer");
        System.out.println("2. Login customer");
        System.out.println("3. Xem san pham dang sale");
        System.out.println("4. Dat hang NO_LOCK");
        System.out.println("5. Xem ket qua/report");
        System.out.println("6. Logout");
        System.out.println("0. Thoat");
        System.out.print("Chon: ");
    }

    private void register() {
        System.out.print("Nhap ten: ");
        String name = scanner.nextLine().trim();
        System.out.print("Nhap email: ");
        String email = scanner.nextLine().trim();
        CustomerTier tier = readTier();

        try {
            Customer customer = customerController.register(name, email, tier);
            System.out.println("Register thanh cong. Customer ID: " + customer.getCustomerId());
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

    private CustomerTier readTier() {
        System.out.println("Chon tier: 1. VIP | 2. PREMIUM | 3. REGULAR");
        System.out.print("Tier: ");
        String input = scanner.nextLine().trim();
        if ("1".equals(input)) {
            return CustomerTier.VIP;
        }
        if ("2".equals(input)) {
            return CustomerTier.PREMIUM;
        }
        return CustomerTier.REGULAR;
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
