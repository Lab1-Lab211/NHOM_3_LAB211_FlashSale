package controller;

import model.Product;
import model.enums.ProductCategory;
import service.ProductService;

import java.util.List;

public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    public List<Product> searchByName(String keyword) { return productService.searchByName(keyword); }
    public List<Product> filterByCategory(ProductCategory category) { return productService.filterByCategory(category); }
    public List<Product> filterByPrice(double minPrice, double maxPrice) { return productService.filterByPrice(minPrice, maxPrice); }
}
