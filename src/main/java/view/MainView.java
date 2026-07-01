package view;

import controller.CustomerController;
import controller.FlashSaleController;
import controller.OrderController;
import controller.SimulatorController;
import model.Customer;
import repository.CustomerRepository;
import repository.FlashSaleEventRepository;
import repository.FlashSaleItemRepository;
import repository.OrderDetailRepository;
import repository.OrderRepository;
import repository.OrderTransactionRepository;
import service.BookingResult;
import service.CustomerService;
import service.FlashSaleItemService;
import service.FlashSaleService;
import service.OrderService;
import service.SimulatorService;

import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class MainView {
    private final ConsoleInput input = new ConsoleInput();
    private final CustomerController customerController;
    private final FlashSaleView flashSaleView;
    private final OrderView orderView;
    private final ReportView reportView;
    private final SimulatorView simulatorView;
    private BookingResult lastBookingResult;

    public MainView() {
        Path dataRoot = resolveDataRoot();
        Path customerCsv = dataRoot.resolve("customers.csv");
        Path eventCsv = dataRoot.resolve("flash_events.csv");
        Path itemCsv = dataRoot.resolve("flash_items.csv");
        Path orderCsv = dataRoot.resolve("orders.csv");
        Path orderDetailCsv = dataRoot.resolve("order_details.csv");
        Path transactionCsv = dataRoot.resolve("transactions.csv");

        if (!Files.exists(eventCsv) || !Files.exists(itemCsv)) {
            System.err.println("Khong tim thay file data flash sale. Duong dan dang su dung: " + dataRoot);
        }

        CustomerRepository customerRepository = new CustomerRepository(customerCsv.toString());
        FlashSaleEventRepository eventRepository = new FlashSaleEventRepository(eventCsv.toString());
        FlashSaleItemRepository itemRepository = new FlashSaleItemRepository(itemCsv.toString());
        OrderRepository orderRepository = new OrderRepository(orderCsv.toString());
        OrderDetailRepository orderDetailRepository = new OrderDetailRepository(orderDetailCsv.toString());
        OrderTransactionRepository transactionRepository = new OrderTransactionRepository(transactionCsv.toString());

        CustomerService customerService = new CustomerService(customerRepository);
        FlashSaleItemService itemService = new FlashSaleItemService(itemRepository, eventRepository);
        FlashSaleService flashSaleService = new FlashSaleService(eventRepository, itemService);
        OrderService orderService = new OrderService(
                orderRepository, orderDetailRepository, itemRepository, eventRepository, customerRepository);
        SimulatorService simulatorService = new SimulatorService(itemRepository, transactionRepository);

        this.customerController = new CustomerController(customerService);
        FlashSaleController flashSaleController = new FlashSaleController(flashSaleService);
        OrderController orderController = new OrderController(orderService);
        SimulatorController simulatorController = new SimulatorController(simulatorService);

        this.flashSaleView = new FlashSaleView(flashSaleController);
        this.orderView = new OrderView(orderController, customerController, input);
        this.reportView = new ReportView();
        this.simulatorView = new SimulatorView(simulatorController, input);
    }

    public static void main(String[] args) {
        configureUtf8Console();
        new MainView().run();
    }

    public void run() {
        boolean running = true;
        while (running) {
            showMenu();
            String choice = input.readLine().trim();
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
                    lastBookingResult = orderView.placeOrderWithSelectedMechanism();
                    reportView.showBookingResult(lastBookingResult);
                    break;
                case "5":
                    reportView.showBookingResult(lastBookingResult);
                    break;
                case "6":
                    try {
                        simulatorView.runInteractive();
                    } catch (IllegalArgumentException | IllegalStateException e) {
                        System.out.println("Chay simulator that bai: " + e.getMessage());
                    }
                    break;
                case "7":
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
                    + " - " + customerController.getCurrentCustomer().getName()
                    + " | Role: " + customerController.getCurrentRole().getDisplayName());
        } else {
            System.out.println("Chua login.");
        }
        System.out.println("1. Register customer");
        System.out.println("2. Login customer");
        System.out.println("3. Xem san pham dang sale");
        System.out.println("4. Dat hang (chon co che lock)");
        System.out.println("5. Xem ket qua/report");
        System.out.println("6. Chay Simulator 4 co che lock");
        System.out.println("7. Logout");
        System.out.println("0. Thoat");
        System.out.print("Chon: ");
    }

    private void register() {
        String name = input.readLine("Nhap ten: ").trim();
        String email = input.readLine("Nhap email: ").trim();

        try {
            Customer customer = customerController.register(name, email);
            System.out.println("Register thanh cong. Customer ID: " + customer.getCustomerId()
                    + " | Tier mac dinh: " + customer.getTier());
        } catch (IllegalArgumentException e) {
            System.out.println("Register that bai: " + e.getMessage());
        }
    }

    private void login() {
        String email = input.readLine("Nhap email: ").trim();
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
            new ProcessBuilder("cmd", "/c", "chcp", "65001")
                    .redirectErrorStream(true)
                    .start()
                    .waitFor();
        } catch (Exception ignored) {
            // Neu khong doi duoc code page, van fallback bang Console/UTF-8 scanner.
        }
    }
}
