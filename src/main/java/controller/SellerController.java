package controller;

import model.FlashSaleEvent;
import model.FlashSaleItem;
import model.Product;
import model.Order;
import model.OrderDetail;
import model.Seller;
import model.enums.OrderStatus;
import model.enums.ProductCategory;
import exception.EntityNotFoundException;
import service.OrderService;
import service.SellerService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class SellerController {
    private final SellerService sellerService;
    private final OrderService orderService;
    private Seller currentSeller;

    public SellerController(SellerService sellerService) {
        this(sellerService, null);
    }

    public SellerController(SellerService sellerService, OrderService orderService) {
        this.sellerService = sellerService;
        this.orderService = orderService;
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

    public Optional<FlashSaleEvent> findOwnEvent(String eventId) {
        return sellerService.findOwnEvent(currentSeller, eventId);
    }

    public FlashSaleEvent updateEvent(String eventId, String name, String start,
                                      String end, Integer discount) {
        return sellerService.updateOwnEvent(currentSeller, eventId, name, start, end, discount);
    }

    public boolean removeItem(String eventId, String flashItemId) {
        return sellerService.removeItemFromOwnEvent(currentSeller, eventId, flashItemId);
    }

    public FlashSaleEvent resubmit(String eventId) {
        return sellerService.resubmitRejectedEvent(currentSeller, eventId);
    }

    public Optional<Product> findProductById(String productId) {
        return sellerService.findProductById(productId);
    }

    public List<Order> getReceivedOrders() {
        requireOrderService();
        return orderService.getOrdersForSeller(currentSeller);
    }

    public Order getReceivedOrder(String orderId) throws EntityNotFoundException {
        requireOrderService();
        return orderService.getOrderForSeller(currentSeller, orderId);
    }

    public List<OrderDetail> getReceivedOrderDetails(String orderId) throws EntityNotFoundException {
        requireOrderService();
        return orderService.getOrderDetailsForSeller(currentSeller, orderId);
    }

    public Optional<FlashSaleItem> findFlashItemById(String flashItemId) {
        requireOrderService();
        return orderService.findFlashSaleItem(flashItemId);
    }

    public Order updateOrderStatus(String orderId, OrderStatus status)
            throws EntityNotFoundException {
        requireOrderService();
        return orderService.updateOrderStatusForSeller(currentSeller, orderId, status);
    }

    private void requireOrderService() {
        if (orderService == null) {
            throw new IllegalStateException("Chuc nang don hang nguoi ban chua duoc khoi tao");
        }
    }
}
