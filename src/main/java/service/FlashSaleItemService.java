package service;

import model.FlashSaleEvent;
import model.FlashSaleItem;
import model.enums.SaleStatus;
import repository.FlashSaleEventRepository;
import repository.FlashSaleItemRepository;
import repository.ProductRepository;
import model.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FlashSaleItemService {
    private final FlashSaleItemRepository flashSaleItemRepository;
    private final FlashSaleEventRepository flashSaleEventRepository;
    private final ProductRepository productRepository;

    public FlashSaleItemService(FlashSaleItemRepository flashSaleItemRepository,
                                FlashSaleEventRepository flashSaleEventRepository,
                                ProductRepository productRepository) {
        this.flashSaleItemRepository = flashSaleItemRepository;
        this.flashSaleEventRepository = flashSaleEventRepository;
        this.productRepository = productRepository;
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

    public List<FlashSaleItem> getAllItems() {
        return flashSaleItemRepository.findAll();
    }

    public FlashSaleItem addItem(FlashSaleItem item) throws IllegalArgumentException {
        // Check event
        Optional<FlashSaleEvent> eventOpt = flashSaleEventRepository.findById(item.getEventId());
        if (!eventOpt.isPresent()) {
            throw new IllegalArgumentException("Sự kiện Flash Sale không tồn tại!");
        }

        // Check product stock
        Optional<Product> productOpt = productRepository.findById(item.getProductId());
        if (!productOpt.isPresent()) {
            throw new IllegalArgumentException("Sản phẩm không tồn tại trong kho!");
        }

        Product product = productOpt.get();
        if (item.getLimitedQty() > product.getStock()) {
            throw new IllegalArgumentException("Số lượng giới hạn (" + item.getLimitedQty() + ") vượt quá tồn kho hiện tại (" + product.getStock() + ")!");
        }

        // Deduct stock
        product.setStock(product.getStock() - item.getLimitedQty());
        productRepository.update(product);

        // Save item
        flashSaleItemRepository.save(item);
        return item;
    }

    public boolean deleteItem(String flashItemId) {
        Optional<FlashSaleItem> itemOpt = flashSaleItemRepository.findById(flashItemId);
        if (!itemOpt.isPresent()) {
            return false;
        }

        FlashSaleItem item = itemOpt.get();

        // Restore stock for unsold items
        int unsoldQty = item.getLimitedQty() - item.getSoldQty();
        if (unsoldQty > 0) {
            Optional<Product> productOpt = productRepository.findById(item.getProductId());
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                product.setStock(product.getStock() + unsoldQty);
                productRepository.update(product);
            }
        }

        return flashSaleItemRepository.deleteById(flashItemId);
    }
}
