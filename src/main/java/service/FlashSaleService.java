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

    public FlashSaleEvent createEvent(FlashSaleEvent event) {
        flashSaleEventRepository.save(event);
        return event;
    }

    public boolean updateEvent(FlashSaleEvent event) {
        if (flashSaleEventRepository.findById(event.getEventId()).isPresent()) {
            flashSaleEventRepository.update(event);
            return true;
        }
        return false;
    }

    public boolean deleteEvent(String eventId) {
        return flashSaleEventRepository.deleteById(eventId);
    }

    public List<FlashSaleEvent> listActiveEvents() {
        return flashSaleEventRepository.findDangDienRa();
    }

    public List<FlashSaleEvent> listPendingApprovalEvents() {
        return flashSaleEventRepository.findByStatus(SaleStatus.CHO_PHE_DUYET);
    }

    public Optional<FlashSaleEvent> approveEvent(String eventId) {
        Optional<FlashSaleEvent> found = flashSaleEventRepository.findById(eventId);
        if (!found.isPresent()) return Optional.empty();
        FlashSaleEvent event = found.get();
        if (event.getStatus() != SaleStatus.CHO_PHE_DUYET) {
            throw new IllegalArgumentException("Chi phe duyet Flash Sale dang cho duyet");
        }
        if (flashSaleItemService.listItemsByEvent(eventId).isEmpty()) {
            throw new IllegalArgumentException("Flash Sale chua co hang hoa de phe duyet");
        }
        event.setStatus(SaleStatus.SAP_DIEN_RA);
        flashSaleEventRepository.update(event);
        return Optional.of(event);
    }

    public Optional<FlashSaleEvent> rejectEvent(String eventId) {
        Optional<FlashSaleEvent> found = flashSaleEventRepository.findById(eventId);
        if (!found.isPresent()) return Optional.empty();
        FlashSaleEvent event = found.get();
        if (event.getStatus() != SaleStatus.CHO_PHE_DUYET) {
            throw new IllegalArgumentException("Chi tu choi Flash Sale dang cho duyet");
        }
        for (FlashSaleItem item : flashSaleItemService.listItemsByEvent(eventId)) {
            flashSaleItemService.deleteItem(item.getFlashItemId());
        }
        event.setStatus(SaleStatus.TU_CHOI);
        flashSaleEventRepository.update(event);
        return Optional.of(event);
    }

    public List<FlashSaleItem> listActiveAvailableItems() {
        return flashSaleItemService.listActiveAvailableItems();
    }

    public List<FlashSaleItem> listActiveAvailableItemsByEvent(String eventId) {
        return flashSaleItemService.listActiveAvailableItemsByEvent(eventId);
    }

    public List<FlashSaleItem> getAllItems() {
        return flashSaleItemService.getAllItems();
    }

    public FlashSaleItem addItem(FlashSaleItem item) {
        return flashSaleItemService.addItem(item);
    }

    public boolean deleteItem(String flashItemId) {
        return flashSaleItemService.deleteItem(flashItemId);
    }

    public Optional<FlashSaleEvent> startEvent(String eventId) {
        Optional<FlashSaleEvent> event = flashSaleEventRepository.findById(eventId);
        if (event.isPresent() && event.get().getStatus() != SaleStatus.SAP_DIEN_RA) {
            throw new IllegalArgumentException("Chi duoc bat dau Flash Sale da duoc phe duyet");
        }
        return changeStatus(eventId, SaleStatus.DANG_DIEN_RA);
    }

    public Optional<FlashSaleEvent> endEvent(String eventId) {
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
