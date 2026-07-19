package service;

import model.FlashSaleEvent;
import model.FlashSaleItem;
import model.Product;
import model.Seller;
import model.enums.ProductCategory;
import model.enums.SaleStatus;
import repository.FlashSaleEventRepository;
import repository.FlashSaleItemRepository;
import repository.ProductRepository;
import repository.SellerRepository;
import util.PasswordHasher;
import util.TextEncodingFixer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class SellerService {
    private static final java.util.regex.Pattern EMAIL_PATTERN =
            java.util.regex.Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    private final SellerRepository sellerRepository;
    private final ProductRepository productRepository;
    private final FlashSaleEventRepository eventRepository;
    private final FlashSaleItemRepository itemRepository;
    private final FlashSaleItemService itemService;

    public SellerService(SellerRepository sellerRepository,
                         ProductRepository productRepository,
                         FlashSaleEventRepository eventRepository,
                         FlashSaleItemRepository itemRepository,
                         FlashSaleItemService itemService) {
        this.sellerRepository = sellerRepository;
        this.productRepository = productRepository;
        this.eventRepository = eventRepository;
        this.itemRepository = itemRepository;
        this.itemService = itemService;
    }

    public Seller register(String name, String email, String password) {
        validateText(name, "Ten nguoi ban");
        validateText(email, "Email");
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Dinh dang email khong hop le");
        }
        if (password == null || password.trim().length() < 6) {
            throw new IllegalArgumentException("Mat khau phai co it nhat 6 ky tu");
        }
        if (sellerRepository.findByEmail(email.trim()).isPresent()) {
            throw new IllegalArgumentException("Email nguoi ban da ton tai");
        }
        PasswordHasher.PasswordData data = PasswordHasher.hash(password);
        Seller seller = new Seller(nextSellerId(), cleanCsvText(name), email.trim(),
                data.getHash(), data.getSalt(), Collections.emptyList(), Collections.emptyList(),
                LocalDate.now().toString());
        sellerRepository.save(seller);
        return seller;
    }

    public Optional<Seller> login(String email, String password) {
        if (email == null || password == null) return Optional.empty();
        Optional<Seller> found = sellerRepository.findByEmail(email.trim());
        if (!found.isPresent()) return Optional.empty();
        Seller seller = found.get();
        return PasswordHasher.verify(password, seller.getPasswordHash(), seller.getPasswordSalt())
                ? found : Optional.empty();
    }

    public Product addProduct(Seller seller, String name, ProductCategory category,
                              double originalPrice, int stock) {
        requireSeller(seller);
        validateText(name, "Ten san pham");
        if (category == null) throw new IllegalArgumentException("Danh muc khong hop le");
        if (originalPrice <= 0) throw new IllegalArgumentException("Gia phai lon hon 0");
        if (stock < 0) throw new IllegalArgumentException("Ton kho khong duoc am");

        Product product = new Product(nextProductId(), cleanCsvText(name), category,
                originalPrice, stock, 1);
        productRepository.save(product);
        seller.addProductId(product.getProductId());
        sellerRepository.update(seller);
        return product;
    }

    public List<Product> getOwnProducts(Seller seller) {
        requireSeller(seller);
        List<Product> result = new ArrayList<>();
        for (String id : seller.getProductIds()) {
            productRepository.findById(id).ifPresent(result::add);
        }
        return result;
    }

    public FlashSaleEvent createFlashSaleRequest(Seller seller, String name,
                                                  String startTime, String endTime,
                                                  int discountPercent) {
        requireSeller(seller);
        validateText(name, "Ten Flash Sale");
        LocalDateTime start = parseTime(startTime, "Thoi gian bat dau");
        LocalDateTime end = parseTime(endTime, "Thoi gian ket thuc");
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("Thoi gian ket thuc phai sau thoi gian bat dau");
        }
        if (discountPercent <= 0 || discountPercent >= 100) {
            throw new IllegalArgumentException("Phan tram giam phai trong khoang 1-99");
        }

        FlashSaleEvent event = new FlashSaleEvent(nextEventId(), cleanCsvText(name),
                startTime.trim(), endTime.trim(), SaleStatus.SAP_DIEN_RA, discountPercent);
        eventRepository.save(event);
        seller.addEventId(event.getEventId());
        sellerRepository.update(seller);
        return event;
    }

    public FlashSaleItem addItemToOwnEvent(Seller seller, String eventId, String productId,
                                            int limitedQty, double flashPrice) {
        requireSeller(seller);
        if (!seller.ownsEvent(eventId)) {
            throw new IllegalArgumentException("Flash Sale khong thuoc nguoi ban nay");
        }
        if (!seller.ownsProduct(productId)) {
            throw new IllegalArgumentException("San pham khong thuoc nguoi ban nay");
        }
        FlashSaleEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay Flash Sale"));
        if (event.getStatus() != SaleStatus.SAP_DIEN_RA) {
            throw new IllegalArgumentException("Chi duoc them hang khi Flash Sale sap dien ra");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay san pham"));
        if (limitedQty <= 0) throw new IllegalArgumentException("So luong phai lon hon 0");
        if (flashPrice <= 0 || flashPrice >= product.getOriginalPrice()) {
            throw new IllegalArgumentException("Gia Flash Sale phai lon hon 0 va nho hon gia goc");
        }
        FlashSaleItem item = new FlashSaleItem(nextFlashItemId(), eventId, productId,
                limitedQty, 0, flashPrice, 1);
        return itemService.addItem(item);
    }

    public List<FlashSaleEvent> getOwnEvents(Seller seller) {
        requireSeller(seller);
        List<FlashSaleEvent> result = new ArrayList<>();
        for (String id : seller.getEventIds()) {
            eventRepository.findById(id).ifPresent(result::add);
        }
        return result;
    }

    public List<FlashSaleItem> getItemsByEvent(Seller seller, String eventId) {
        requireSeller(seller);
        if (!seller.ownsEvent(eventId)) {
            throw new IllegalArgumentException("Flash Sale khong thuoc nguoi ban nay");
        }
        return itemRepository.findByEvent(eventId);
    }

    public Optional<FlashSaleEvent> findOwnEvent(Seller seller, String eventId) {
        requireSeller(seller);
        if (!seller.ownsEvent(eventId)) return Optional.empty();
        return eventRepository.findById(eventId);
    }

    public FlashSaleEvent startOwnEvent(Seller seller, String eventId) {
        requireSeller(seller);
        if (!seller.ownsEvent(eventId)) {
            throw new IllegalArgumentException("Flash Sale khong thuoc nguoi ban nay");
        }
        FlashSaleEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay Flash Sale"));
        if (event.getStatus() == SaleStatus.DANG_DIEN_RA) {
            return event;
        }
        if (event.getStatus() == SaleStatus.SAP_DIEN_RA) {
            throw new IllegalArgumentException("Flash Sale chua toi thoi gian bat dau");
        }
        throw new IllegalArgumentException("Chi duoc bat dau Flash Sale sap dien ra");
    }

    public FlashSaleEvent endOwnEvent(Seller seller, String eventId) {
        requireSeller(seller);
        if (!seller.ownsEvent(eventId)) {
            throw new IllegalArgumentException("Flash Sale khong thuoc nguoi ban nay");
        }
        FlashSaleEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay Flash Sale"));
        if (event.getStatus() != SaleStatus.DANG_DIEN_RA) {
            throw new IllegalArgumentException("Chi duoc ket thuc Flash Sale dang dien ra");
        }
        event.setStatus(SaleStatus.DA_KET_THUC);
        eventRepository.update(event);
        return event;
    }

    public FlashSaleEvent updateOwnEvent(Seller seller, String eventId, String name,
                                          String startTime, String endTime, Integer discountPercent) {
        FlashSaleEvent event = requireEditableEvent(seller, eventId);
        String newName = name == null || name.trim().isEmpty() ? event.getEventName() : cleanCsvText(name);
        String newStartText = startTime == null || startTime.trim().isEmpty()
                ? event.getStartTime() : startTime.trim();
        String newEndText = endTime == null || endTime.trim().isEmpty()
                ? event.getEndTime() : endTime.trim();
        int newDiscount = discountPercent == null ? event.getDiscountPercent() : discountPercent;

        LocalDateTime start = parseTime(newStartText, "Thoi gian bat dau");
        LocalDateTime end = parseTime(newEndText, "Thoi gian ket thuc");
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("Thoi gian ket thuc phai sau thoi gian bat dau");
        }
        if (newDiscount <= 0 || newDiscount >= 100) {
            throw new IllegalArgumentException("Phan tram giam phai trong khoang 1-99");
        }

        event.setEventName(newName);
        event.setStartTime(newStartText);
        event.setEndTime(newEndText);
        event.setDiscountPercent(newDiscount);
        eventRepository.update(event);
        return event;
    }

    public boolean removeItemFromOwnEvent(Seller seller, String eventId, String flashItemId) {
        requireEditableEvent(seller, eventId);
        FlashSaleItem item = itemRepository.findById(flashItemId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay Flash Sale Item"));
        if (!eventId.equalsIgnoreCase(item.getEventId())) {
            throw new IllegalArgumentException("Flash Sale Item khong thuoc su kien nay");
        }
        return itemService.deleteItem(flashItemId);
    }

    public Optional<Product> findProductById(String productId) {
        return productRepository.findById(productId);
    }

    private FlashSaleEvent requireEditableEvent(Seller seller, String eventId) {
        requireSeller(seller);
        if (!seller.ownsEvent(eventId)) {
            throw new IllegalArgumentException("Flash Sale khong thuoc nguoi ban nay");
        }
        FlashSaleEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay Flash Sale"));
        if (event.getStatus() != SaleStatus.SAP_DIEN_RA) {
            throw new IllegalArgumentException("Chi duoc sua Flash Sale sap dien ra");
        }
        return event;
    }

    private void requireSeller(Seller seller) {
        if (seller == null) throw new IllegalStateException("Vui long dang nhap nguoi ban");
    }

    private void validateText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " khong duoc trong");
        }
    }

    private String cleanCsvText(String value) {
        return TextEncodingFixer.repairConsoleText(value).trim().replace(',', ' ');
    }

    private LocalDateTime parseTime(String value, String field) {
        try {
            return LocalDateTime.parse(value == null ? "" : value.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(field + " phai theo yyyy-MM-dd'T'HH:mm:ss");
        }
    }

    private String nextSellerId() {
        return nextId("SEL-", sellerRepository.findAll().stream().map(Seller::getSellerId).toArray(String[]::new));
    }

    private String nextProductId() {
        return nextId("PRD-", productRepository.findAll().stream().map(Product::getProductId).toArray(String[]::new));
    }

    private String nextEventId() {
        return nextId("EVT-", eventRepository.findAll().stream().map(FlashSaleEvent::getEventId).toArray(String[]::new));
    }

    private String nextFlashItemId() {
        return nextId("FSI-", itemRepository.findAll().stream().map(FlashSaleItem::getFlashItemId).toArray(String[]::new));
    }

    private String nextId(String prefix, String[] ids) {
        int max = 0;
        for (String id : ids) {
            if (id != null && id.startsWith(prefix)) {
                try { max = Math.max(max, Integer.parseInt(id.substring(prefix.length()))); }
                catch (NumberFormatException ignored) { }
            }
        }
        return String.format("%s%05d", prefix, max + 1);
    }
}
