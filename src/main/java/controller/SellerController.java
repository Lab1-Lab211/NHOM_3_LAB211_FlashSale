package controller;

import model.FlashSaleEvent;
import model.FlashSaleItem;
import model.Product;
import model.Seller;
import model.enums.ProductCategory;
import service.SellerService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class SellerController {
    private final SellerService sellerService;
    private Seller currentSeller;

    public SellerController(SellerService sellerService) {
        this.sellerService = sellerService;
    }

    public Seller register(String name, String email, String password) {
        currentSeller = sellerService.register(name, email, password);
        return currentSeller;
    }

    public boolean login(String email, String password) {
        Optional<Seller> seller = sellerService.login(email, password);
        currentSeller = seller.orElse(null);
        return seller.isPresent();
    }

    public void logout() { currentSeller = null; }
    public boolean isLoggedIn() { return currentSeller != null; }
    public Seller getCurrentSeller() { return currentSeller; }

    public Product addProduct(String name, ProductCategory category, double price, int stock) {
        return sellerService.addProduct(currentSeller, name, category, price, stock);
    }

    public List<Product> getOwnProducts() {
        return currentSeller == null ? Collections.emptyList() : sellerService.getOwnProducts(currentSeller);
    }

    public FlashSaleEvent createFlashSaleRequest(String name, String start, String end, int discount) {
        return sellerService.createFlashSaleRequest(currentSeller, name, start, end, discount);
    }

    public FlashSaleItem addItem(String eventId, String productId, int qty, double price) {
        return sellerService.addItemToOwnEvent(currentSeller, eventId, productId, qty, price);
    }

    public List<FlashSaleEvent> getOwnEvents() {
        return currentSeller == null ? Collections.emptyList() : sellerService.getOwnEvents(currentSeller);
    }

    public List<FlashSaleItem> getItemsByEvent(String eventId) {
        return sellerService.getItemsByEvent(currentSeller, eventId);
    }
}
