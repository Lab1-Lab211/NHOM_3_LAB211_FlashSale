package view;

import controller.FlashSaleController;
import model.FlashSaleEvent;
import model.FlashSaleItem;

import java.util.List;

public class FlashSaleView {
    private final FlashSaleController flashSaleController;

    public FlashSaleView(FlashSaleController flashSaleController) {
        this.flashSaleController = flashSaleController;
    }

    public List<FlashSaleItem> showActiveItems() {
        List<FlashSaleEvent> events = flashSaleController.getActiveEvents();
        List<FlashSaleItem> items = flashSaleController.getActiveItems();

        System.out.println();
        System.out.println("=== FLASH SALE DANG DIEN RA ===");
        if (events.isEmpty()) {
            System.out.println("Khong co event nao dang dien ra.");
        } else {
            for (FlashSaleEvent event : events) {
                System.out.printf("%s | %s | giam %d%%%n",
                        event.getEventId(), event.getEventName(), event.getDiscountPercent());
            }
        }

        System.out.println();
        System.out.println("=== SAN PHAM CON HANG ===");
        if (items.isEmpty()) {
            System.out.println("Khong co san pham con hang trong cac event dang dien ra.");
            return items;
        }

        System.out.printf("%-12s %-10s %-12s %-8s %-8s %-12s%n",
                "FlashItem", "Event", "Product", "Limit", "Sold", "Price");
        for (FlashSaleItem item : items) {
            System.out.printf("%-12s %-10s %-12s %-8d %-8d %-12.0f%n",
                    item.getFlashItemId(),
                    item.getEventId(),
                    item.getProductId(),
                    item.getLimitedQty(),
                    item.getSoldQty(),
                    item.getFlashPrice());
        }
        return items;
    }
}
