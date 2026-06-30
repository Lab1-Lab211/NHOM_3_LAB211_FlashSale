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

    public List<FlashSaleEvent> getActiveEvents() {
        return flashSaleService.listActiveEvents();
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
