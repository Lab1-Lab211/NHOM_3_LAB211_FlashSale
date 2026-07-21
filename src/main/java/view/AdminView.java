package view;

import controller.FlashSaleController;
import model.FlashSaleEvent;

import java.util.List;
import java.util.Optional;

public class AdminView {
    public static final String ADMIN_EMAIL = "admin@gmail.com";
    public static final String ADMIN_PASSWORD = "123456";

    private final FlashSaleController flashSaleController;
    private final SimulatorView simulatorView;
    private final ConsoleInput input;

    public AdminView(FlashSaleController flashSaleController,
                     SimulatorView simulatorView,
                     ConsoleInput input) {
        this.flashSaleController = flashSaleController;
        this.simulatorView = simulatorView;
        this.input = input;
    }

    public void loginAndRun() {
        String email = input.readLine("Admin email: ").trim();
        String password = input.readPassword("Admin password: ");
        if (!ADMIN_EMAIL.equalsIgnoreCase(email) || !ADMIN_PASSWORD.equals(password)) {
            System.out.println("Sai tai khoan Admin.");
            return;
        }

        boolean running = true;
        while (running) {
            System.out.println("\n===== QUAN TRI VIEN =====");
            System.out.println("1. Xem tat ca Flash Sale");
            System.out.println("2. Ket thuc Flash Sale");
            System.out.println("3. Chay Simulator 4 co che lock");
            System.out.println("0. Dang xuat Admin");
            String choice = input.readLine("Chon: ").trim();
            try {
                switch (choice) {
                    case "1": printEvents(flashSaleController.getAllEvents()); break;
                    case "2": endEvent(); break;
                    case "3": simulatorView.runInteractive(); break;
                    case "0": running = false; break;
                    default: System.out.println("Lua chon khong hop le.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("Thao tac that bai: " + e.getMessage());
            }
        }
    }

    private void endEvent() {
        String eventId = input.readLine("Event ID can ket thuc: ").trim();
        Optional<FlashSaleEvent> event = flashSaleController.endEvent(eventId);
        System.out.println(event.isPresent()
                ? "Cap nhat trang thai thanh cong: " + event.get().getStatus().getMoTa()
                : "Khong tim thay Flash Sale.");
    }

    private void printEvents(List<FlashSaleEvent> events) {
        if (events.isEmpty()) {
            System.out.println("Khong co Flash Sale phu hop.");
            return;
        }
        for (FlashSaleEvent event : events) {
            System.out.printf("%s | %s | %s | %s -> %s | giam %d%%%n",
                    event.getEventId(), event.getEventName(), event.getStatus().getMoTa(),
                    event.getStartTime(), event.getEndTime(), event.getDiscountPercent());
        }
    }
}
