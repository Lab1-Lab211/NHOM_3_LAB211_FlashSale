package service;

import model.FlashSaleEvent;
import model.FlashSaleItem;
import model.enums.SaleStatus;
import repository.FlashSaleEventRepository;

import java.util.List;
import java.util.Optional;

public class FlashSaleService {
    private final FlashSaleEventRepository flashSaleEventRepository;
    private final FlashSaleItemService flashSaleItemService;

    public FlashSaleService(FlashSaleEventRepository flashSaleEventRepository,
                            FlashSaleItemService flashSaleItemService) {
        this.flashSaleEventRepository = flashSaleEventRepository;
        this.flashSaleItemService = flashSaleItemService;
    }

    public List<FlashSaleEvent> getAllEvents() {
        return flashSaleEventRepository.findAll();
    }

    public List<FlashSaleEvent> listActiveEvents() {
        return flashSaleEventRepository.findDangDienRa();
    }

    public List<FlashSaleItem> listActiveAvailableItems() {
        return flashSaleItemService.listActiveAvailableItems();
    }

    public String getProductName(String productId) {
        return flashSaleItemService.getProductName(productId);
    }

    public List<FlashSaleItem> listActiveAvailableItemsByEvent(String eventId) {
        return flashSaleItemService.listActiveAvailableItemsByEvent(eventId);
    }

    public Optional<FlashSaleEvent> endEvent(String eventId) {
        Optional<FlashSaleEvent> event = flashSaleEventRepository.findById(eventId);
        if (event.isPresent() && event.get().getStatus() != SaleStatus.DANG_DIEN_RA) {
            throw new IllegalArgumentException("Chi duoc ket thuc Flash Sale dang dien ra");
        }
        return changeStatus(eventId, SaleStatus.DA_KET_THUC);
    }

    private Optional<FlashSaleEvent> changeStatus(String eventId, SaleStatus status) {
        Optional<FlashSaleEvent> event = flashSaleEventRepository.findById(eventId);
        if (event.isPresent()) {
            FlashSaleEvent updated = event.get();
            updated.setStatus(status);
            flashSaleEventRepository.update(updated);
            return Optional.of(updated);
        }
        return Optional.empty();
    }
}
