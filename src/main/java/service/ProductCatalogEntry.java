package service;

import model.FlashSaleEvent;
import model.FlashSaleItem;
import model.Product;

/** Du lieu hien thi san pham kem thong tin Flash Sale dang hoat dong. */
public class ProductCatalogEntry {
    private final Product product;
    private final FlashSaleItem saleItem;
    private final FlashSaleEvent saleEvent;

    public ProductCatalogEntry(Product product, FlashSaleItem saleItem, FlashSaleEvent saleEvent) {
        this.product = product;
        this.saleItem = saleItem;
        this.saleEvent = saleEvent;
    }

    public Product getProduct() { return product; }
    public FlashSaleItem getSaleItem() { return saleItem; }
    public FlashSaleEvent getSaleEvent() { return saleEvent; }
    public boolean isOnSale() { return saleItem != null && saleEvent != null; }
    public double getDisplayPrice() { return isOnSale() ? saleItem.getFlashPrice() : product.getOriginalPrice(); }
}
