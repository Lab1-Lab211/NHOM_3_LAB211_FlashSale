package controller;

import model.FlashSaleEvent;
import model.FlashSaleItem;
import service.FlashSaleService;

import java.util.List;
import java.util.Optional;

public class FlashSaleController {
    private final FlashSaleService flashSaleService;

    public FlashSaleController(FlashSaleService flashSaleService) {
        this.flashSaleService = flashSaleService;
    }

    public List<FlashSaleEvent> getAllEvents() {
        return flashSaleService.getAllEvents();
    }

    public FlashSaleEvent createEvent(FlashSaleEvent event) {
        return flashSaleService.createEvent(event);
    }

    public boolean updateEvent(FlashSaleEvent event) {
        return flashSaleService.updateEvent(event);
    }

    public boolean deleteEvent(String eventId) {
        return flashSaleService.deleteEvent(eventId);
    }

    public List<FlashSaleEvent> getActiveEvents() {
        return flashSaleService.listActiveEvents();
    }

    public List<FlashSaleItem> getAllItems() {
        return flashSaleService.getAllItems();
    }

    public FlashSaleItem addItem(FlashSaleItem item) {
        return flashSaleService.addItem(item);
    }

    public boolean deleteItem(String flashItemId) {
        return flashSaleService.deleteItem(flashItemId);
    }

    public List<FlashSaleItem> getActiveItems() {
        return flashSaleService.listActiveAvailableItems();
    }

    public List<FlashSaleItem> getActiveItemsByEvent(String eventId) {
        return flashSaleService.listActiveAvailableItemsByEvent(eventId);
    }

    public Optional<FlashSaleEvent> startEvent(String eventId) {
        return flashSaleService.startEvent(eventId);
    }

    public Optional<FlashSaleEvent> endEvent(String eventId) {
        return flashSaleService.endEvent(eventId);
    }
}
