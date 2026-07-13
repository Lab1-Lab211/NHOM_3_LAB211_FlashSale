package service;

import model.Product;
import model.FlashSaleEvent;
import model.FlashSaleItem;
import model.enums.ProductCategory;
import model.enums.SaleStatus;
import repository.FlashSaleEventRepository;
import repository.FlashSaleItemRepository;
import repository.ProductRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ProductService {
    private final ProductRepository productRepository;
    private final FlashSaleItemRepository itemRepository;
    private final FlashSaleEventRepository eventRepository;

    public ProductService(ProductRepository productRepository) {
        this(productRepository, null, null);
    }

    public ProductService(ProductRepository productRepository,
                          FlashSaleItemRepository itemRepository,
                          FlashSaleEventRepository eventRepository) {
        this.productRepository = productRepository;
        this.itemRepository = itemRepository;
        this.eventRepository = eventRepository;
    }

    public List<ProductCatalogEntry> searchByName(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("Tu khoa khong duoc trong");
        }
        return decorate(productRepository.findByName(keyword.trim()));
    }

    public List<ProductCatalogEntry> filterByCategory(ProductCategory category) {
        if (category == null) throw new IllegalArgumentException("Danh muc khong hop le");
        return decorate(productRepository.findByCategory(category));
    }

    public List<ProductCatalogEntry> filterByPrice(double minPrice, double maxPrice) {
        if (minPrice < 0 || maxPrice < minPrice) {
            throw new IllegalArgumentException("Khoang gia khong hop le");
        }
        return decorate(productRepository.findAll()).stream()
                .filter(entry -> entry.getDisplayPrice() >= minPrice && entry.getDisplayPrice() <= maxPrice)
                .collect(Collectors.toList());
    }

    private List<ProductCatalogEntry> decorate(List<Product> products) {
        List<ProductCatalogEntry> result = new ArrayList<>();
        for (Product product : products) {
            result.add(createEntry(product));
        }
        return result;
    }

    private ProductCatalogEntry createEntry(Product product) {
        if (itemRepository == null || eventRepository == null) {
            return new ProductCatalogEntry(product, null, null);
        }
        Optional<FlashSaleItem> bestItem = itemRepository.findByProduct(product.getProductId()).stream()
                .filter(item -> item.soLuongConLai() > 0)
                .filter(item -> eventRepository.findById(item.getEventId())
                        .map(event -> event.getStatus() == SaleStatus.DANG_DIEN_RA)
                        .orElse(false))
                .min(Comparator.comparingDouble(FlashSaleItem::getFlashPrice));
        if (!bestItem.isPresent()) {
            return new ProductCatalogEntry(product, null, null);
        }
        FlashSaleItem item = bestItem.get();
        FlashSaleEvent event = eventRepository.findById(item.getEventId()).orElse(null);
        return new ProductCatalogEntry(product, item, event);
    }
}
