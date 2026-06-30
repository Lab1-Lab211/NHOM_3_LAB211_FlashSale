package view;

import controller.FlashSaleController;
import model.FlashSaleEvent;
import model.FlashSaleItem;
import model.enums.SaleStatus;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class AdminFlashSaleView {
    private final FlashSaleController flashSaleController;
    private final Scanner scanner;

    public AdminFlashSaleView(FlashSaleController flashSaleController, Scanner scanner) {
        this.flashSaleController = flashSaleController;
        this.scanner = scanner;
    }

    public void showAdminMenu() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("===== ADMIN - QUẢN LÝ FLASH SALE =====");
            System.out.println("1. Xem tất cả sự kiện");
            System.out.println("2. Thêm sự kiện mới");
            System.out.println("3. Cập nhật sự kiện");
            System.out.println("4. Xóa sự kiện");
            System.out.println("5. Bắt đầu/Kết thúc sự kiện");
            System.out.println("6. Xem danh sách sản phẩm Flash Sale");
            System.out.println("7. Thêm sản phẩm vào sự kiện");
            System.out.println("8. Xóa sản phẩm khỏi sự kiện");
            System.out.println("0. Quay lại");
            System.out.print("Chọn: ");
            
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    listAllEvents();
                    break;
                case "2":
                    addEvent();
                    break;
                case "3":
                    updateEvent();
                    break;
                case "4":
                    deleteEvent();
                    break;
                case "5":
                    changeEventStatus();
                    break;
                case "6":
                    listAllItems();
                    break;
                case "7":
                    addItem();
                    break;
                case "8":
                    deleteItem();
                    break;
                case "0":
                    running = false;
                    break;
                default:
                    System.out.println("Lựa chọn không hợp lệ.");
            }
        }
    }

    private void listAllEvents() {
        List<FlashSaleEvent> events = flashSaleController.getAllEvents();
        System.out.println("\n--- DANH SÁCH SỰ KIỆN FLASH SALE ---");
        if (events.isEmpty()) {
            System.out.println("Chưa có sự kiện nào.");
            return;
        }
        for (FlashSaleEvent e : events) {
            System.out.printf("ID: %-10s | Tên: %-20s | Bắt đầu: %-20s | Kết thúc: %-20s | %-12s | Giảm: %d%%%n",
                    e.getEventId(), e.getEventName(), e.getStartTime(), e.getEndTime(), e.getStatus().getMoTa(), e.getDiscountPercent());
        }
    }

    private void addEvent() {
        System.out.println("\n--- THÊM SỰ KIỆN MỚI ---");
        System.out.print("Nhập Event ID: ");
        String id = scanner.nextLine().trim();
        System.out.print("Nhập tên sự kiện: ");
        String name = scanner.nextLine().trim();
        System.out.print("Nhập thời gian bắt đầu (yyyy-MM-dd'T'HH:mm:ss): ");
        String start = scanner.nextLine().trim();
        System.out.print("Nhập thời gian kết thúc (yyyy-MM-dd'T'HH:mm:ss): ");
        String end = scanner.nextLine().trim();
        System.out.print("Nhập phần trăm giảm giá (30-70): ");
        int discount = 0;
        try {
            discount = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Phần trăm giảm giá không hợp lệ.");
            return;
        }
        
        FlashSaleEvent newEvent = new FlashSaleEvent(id, name, start, end, SaleStatus.SAP_DIEN_RA, discount);
        flashSaleController.createEvent(newEvent);
        System.out.println("Thêm thành công!");
    }

    private void updateEvent() {
        System.out.println("\n--- CẬP NHẬT SỰ KIỆN ---");
        System.out.print("Nhập Event ID cần cập nhật: ");
        String id = scanner.nextLine().trim();
        
        List<FlashSaleEvent> events = flashSaleController.getAllEvents();
        Optional<FlashSaleEvent> existing = events.stream().filter(e -> e.getEventId().equals(id)).findFirst();
        
        if (!existing.isPresent()) {
            System.out.println("Không tìm thấy sự kiện với ID này.");
            return;
        }
        
        FlashSaleEvent event = existing.get();
        
        System.out.print("Nhập tên sự kiện (" + event.getEventName() + ") [Enter để bỏ qua]: ");
        String name = scanner.nextLine().trim();
        if (!name.isEmpty()) event.setEventName(name);
        
        System.out.print("Nhập thời gian bắt đầu (" + event.getStartTime() + ") [Enter để bỏ qua]: ");
        String start = scanner.nextLine().trim();
        if (!start.isEmpty()) event.setStartTime(start);
        
        System.out.print("Nhập thời gian kết thúc (" + event.getEndTime() + ") [Enter để bỏ qua]: ");
        String end = scanner.nextLine().trim();
        if (!end.isEmpty()) event.setEndTime(end);
        
        System.out.print("Nhập phần trăm giảm giá (" + event.getDiscountPercent() + ") [Enter để bỏ qua]: ");
        String discountStr = scanner.nextLine().trim();
        if (!discountStr.isEmpty()) {
            try {
                event.setDiscountPercent(Integer.parseInt(discountStr));
            } catch (NumberFormatException e) {
                System.out.println("Phần trăm giảm giá không hợp lệ. Không cập nhật giảm giá.");
            }
        }
        
        boolean updated = flashSaleController.updateEvent(event);
        if (updated) {
            System.out.println("Cập nhật thành công!");
        } else {
            System.out.println("Cập nhật thất bại.");
        }
    }

    private void deleteEvent() {
        System.out.println("\n--- XÓA SỰ KIỆN ---");
        System.out.print("Nhập Event ID cần xóa: ");
        String id = scanner.nextLine().trim();
        boolean deleted = flashSaleController.deleteEvent(id);
        if (deleted) {
            System.out.println("Xóa thành công!");
        } else {
            System.out.println("Không tìm thấy sự kiện để xóa.");
        }
    }

    private void changeEventStatus() {
        System.out.println("\n--- ĐỔI TRẠNG THÁI SỰ KIỆN ---");
        System.out.print("Nhập Event ID: ");
        String id = scanner.nextLine().trim();
        System.out.println("1. Bắt đầu sự kiện (DANG_DIEN_RA)");
        System.out.println("2. Kết thúc sự kiện (DA_KET_THUC)");
        System.out.print("Chọn: ");
        String choice = scanner.nextLine().trim();
        
        Optional<FlashSaleEvent> res = Optional.empty();
        if ("1".equals(choice)) {
            res = flashSaleController.startEvent(id);
        } else if ("2".equals(choice)) {
            res = flashSaleController.endEvent(id);
        } else {
            System.out.println("Lựa chọn không hợp lệ.");
            return;
        }
        
        if (res.isPresent()) {
            System.out.println("Đổi trạng thái thành công! Trạng thái mới: " + res.get().getStatus().getMoTa());
        } else {
            System.out.println("Thất bại. Không tìm thấy sự kiện với ID này.");
        }
    }

    private void listAllItems() {
        List<FlashSaleItem> items = flashSaleController.getAllItems();
        System.out.println("\n--- DANH SÁCH SẢN PHẨM FLASH SALE ---");
        if (items.isEmpty()) {
            System.out.println("Chưa có sản phẩm nào trong các sự kiện.");
            return;
        }
        System.out.printf("%-15s | %-10s | %-10s | %-10s | %-10s | %-15s%n",
                "FlashItemID", "EventID", "ProductID", "Giới Hạn", "Đã Bán", "Giá Flash Sale");
        for (FlashSaleItem i : items) {
            System.out.printf("%-15s | %-10s | %-10s | %-10d | %-10d | %-15.0f%n",
                    i.getFlashItemId(), i.getEventId(), i.getProductId(), i.getLimitedQty(), i.getSoldQty(), i.getFlashPrice());
        }
    }

    private void addItem() {
        System.out.println("\n--- THÊM SẢN PHẨM VÀO SỰ KIỆN ---");
        System.out.print("Nhập Flash Item ID: ");
        String itemId = scanner.nextLine().trim();
        System.out.print("Nhập Event ID: ");
        String eventId = scanner.nextLine().trim();
        System.out.print("Nhập Product ID (Kho gốc): ");
        String productId = scanner.nextLine().trim();
        
        System.out.print("Nhập số lượng giới hạn (limitedQty): ");
        int limitedQty = 0;
        try {
            limitedQty = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Số lượng không hợp lệ.");
            return;
        }

        System.out.print("Nhập giá Flash Sale: ");
        double flashPrice = 0;
        try {
            flashPrice = Double.parseDouble(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Giá không hợp lệ.");
            return;
        }

        FlashSaleItem newItem = new FlashSaleItem(itemId, eventId, productId, limitedQty, 0, flashPrice, 1);
        try {
            flashSaleController.addItem(newItem);
            System.out.println("Thêm sản phẩm vào sự kiện thành công! Đã trừ tồn kho gốc.");
        } catch (IllegalArgumentException e) {
            System.out.println("Lỗi: " + e.getMessage());
        }
    }

    private void deleteItem() {
        System.out.println("\n--- XÓA SẢN PHẨM KHỎI SỰ KIỆN ---");
        System.out.print("Nhập Flash Item ID cần xóa: ");
        String id = scanner.nextLine().trim();
        boolean deleted = flashSaleController.deleteItem(id);
        if (deleted) {
            System.out.println("Xóa thành công! Số lượng chưa bán đã được hoàn trả về kho gốc.");
        } else {
            System.out.println("Không tìm thấy sản phẩm với ID này.");
        }
    }
}
