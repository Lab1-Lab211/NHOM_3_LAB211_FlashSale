package service;

import model.Product;
import model.enums.ProductCategory;
import repository.ProductRepository;

import java.util.List;

public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> searchByName(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("Tu khoa khong duoc trong");
        }
        return productRepository.findByName(keyword.trim());
    }

    public List<Product> filterByCategory(ProductCategory category) {
        if (category == null) throw new IllegalArgumentException("Danh muc khong hop le");
        return productRepository.findByCategory(category);
    }

    public List<Product> filterByPrice(double minPrice, double maxPrice) {
        if (minPrice < 0 || maxPrice < minPrice) {
            throw new IllegalArgumentException("Khoang gia khong hop le");
        }
        return productRepository.findByPriceRange(minPrice, maxPrice);
    }
}
