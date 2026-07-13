package controller;

import model.enums.ProductCategory;
import service.ProductCatalogEntry;
import service.ProductService;

import java.util.List;

public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    public List<ProductCatalogEntry> searchByName(String keyword) { return productService.searchByName(keyword); }
    public List<ProductCatalogEntry> filterByCategory(ProductCategory category) { return productService.filterByCategory(category); }
    public List<ProductCatalogEntry> filterByPrice(double minPrice, double maxPrice) { return productService.filterByPrice(minPrice, maxPrice); }
}
