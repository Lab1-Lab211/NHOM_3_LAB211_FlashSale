package service;

import model.FlashSaleEvent;
import model.FlashSaleItem;
import model.enums.SaleStatus;
import repository.FlashSaleEventRepository;
import repository.FlashSaleItemRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FlashSaleItemService {
    private final FlashSaleItemRepository flashSaleItemRepository;
    private final FlashSaleEventRepository flashSaleEventRepository;

    public FlashSaleItemService(FlashSaleItemRepository flashSaleItemRepository,
                                FlashSaleEventRepository flashSaleEventRepository) {
        this.flashSaleItemRepository = flashSaleItemRepository;
        this.flashSaleEventRepository = flashSaleEventRepository;
    }

    public List<FlashSaleItem> listActiveAvailableItems() {
        List<FlashSaleItem> result = new ArrayList<>();
        for (FlashSaleItem item : flashSaleItemRepository.findAvailable()) {
            Optional<FlashSaleEvent> event = flashSaleEventRepository.findById(item.getEventId());
            if (event.isPresent() && event.get().getStatus() == SaleStatus.DANG_DIEN_RA) {
                result.add(item);
            }
        }
        return result;
    }

    public List<FlashSaleItem> listActiveAvailableItemsByEvent(String eventId) {
        List<FlashSaleItem> result = new ArrayList<>();
        for (FlashSaleItem item : listActiveAvailableItems()) {
            if (item.getEventId().equalsIgnoreCase(eventId)) {
                result.add(item);
            }
        }
        return result;
    }

    public Optional<FlashSaleItem> findById(String flashItemId) {
        return flashSaleItemRepository.findById(flashItemId);
    }
}
