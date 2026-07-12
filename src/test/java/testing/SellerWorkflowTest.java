package testing;

import model.FlashSaleEvent;
import model.FlashSaleItem;
import model.Product;
import model.Seller;
import model.enums.ProductCategory;
import model.enums.SaleStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import repository.FlashSaleEventRepository;
import repository.FlashSaleItemRepository;
import repository.ProductRepository;
import repository.SellerRepository;
import service.FlashSaleItemService;
import service.FlashSaleService;
import service.SellerService;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class SellerWorkflowTest {
    private static final String DIR = "data/test_seller";
    private static final String SELLERS = DIR + "/sellers.csv";
    private static final String PRODUCTS = DIR + "/products.csv";
    private static final String EVENTS = DIR + "/events.csv";
    private static final String ITEMS = DIR + "/items.csv";

    @AfterEach
    void cleanup() {
        new File(SELLERS).delete();
        new File(PRODUCTS).delete();
        new File(EVENTS).delete();
        new File(ITEMS).delete();
        new File(DIR).delete();
    }

    @Test
    void sellerRegistersCreatesProductAndSubmitsFlashSaleForAdminApproval() {
        SellerRepository sellerRepo = new SellerRepository(SELLERS);
        ProductRepository productRepo = new ProductRepository(PRODUCTS);
        FlashSaleEventRepository eventRepo = new FlashSaleEventRepository(EVENTS);
        FlashSaleItemRepository itemRepo = new FlashSaleItemRepository(ITEMS);
        FlashSaleItemService itemService = new FlashSaleItemService(itemRepo, eventRepo, productRepo);
        SellerService sellerService = new SellerService(
                sellerRepo, productRepo, eventRepo, itemRepo, itemService);

        Seller seller = sellerService.register("Shop A", "shop@example.com", "123456");
        assertFalse(seller.getPasswordHash().contains("123456"));
        assertTrue(sellerService.login("shop@example.com", "123456").isPresent());

        Product product = sellerService.addProduct(
                seller, "Ao Flash Sale", ProductCategory.THOI_TRANG, 200000, 10);
        FlashSaleEvent event = sellerService.createFlashSaleRequest(
                seller, "Sale cua Shop A", "2027-01-01T08:00:00", "2027-01-01T10:00:00", 30);
        FlashSaleItem item = sellerService.addItemToOwnEvent(
                seller, event.getEventId(), product.getProductId(), 5, 140000);

        assertTrue(seller.ownsProduct(product.getProductId()));
        assertTrue(seller.ownsEvent(event.getEventId()));
        assertEquals(SaleStatus.CHO_PHE_DUYET, event.getStatus());
        assertEquals(5, item.getLimitedQty());

        FlashSaleService adminService = new FlashSaleService(eventRepo, itemService);
        FlashSaleEvent approved = adminService.approveEvent(event.getEventId()).orElseThrow(AssertionError::new);
        assertEquals(SaleStatus.SAP_DIEN_RA, approved.getStatus());
    }

    @Test
    void rejectedFlashSaleCanBeEditedItemsRemovedAndResubmitted() {
        cleanup();
        SellerRepository sellerRepo = new SellerRepository(SELLERS);
        ProductRepository productRepo = new ProductRepository(PRODUCTS);
        FlashSaleEventRepository eventRepo = new FlashSaleEventRepository(EVENTS);
        FlashSaleItemRepository itemRepo = new FlashSaleItemRepository(ITEMS);
        FlashSaleItemService itemService = new FlashSaleItemService(itemRepo, eventRepo, productRepo);
        SellerService sellerService = new SellerService(
                sellerRepo, productRepo, eventRepo, itemRepo, itemService);

        Seller seller = sellerService.register("Shop B", "shopb@example.com", "123456");
        Product product = sellerService.addProduct(
                seller, "May nuoc nong", ProductCategory.GIA_DUNG, 1000000, 20);
        FlashSaleEvent event = sellerService.createFlashSaleRequest(
                seller, "Sale lan dau", "2027-02-01T08:00:00", "2027-02-01T10:00:00", 20);
        sellerService.addItemToOwnEvent(seller, event.getEventId(), product.getProductId(), 5, 800000);

        FlashSaleService adminService = new FlashSaleService(eventRepo, itemService);
        assertEquals(SaleStatus.TU_CHOI,
                adminService.rejectEvent(event.getEventId()).get().getStatus());
        assertTrue(itemRepo.findByEvent(event.getEventId()).isEmpty());

        FlashSaleEvent updated = sellerService.updateOwnEvent(
                seller, event.getEventId(), "Sale da chinh sua", "", "", 30);
        assertEquals("Sale da chinh sua", updated.getEventName());

        FlashSaleItem newItem = sellerService.addItemToOwnEvent(
                seller, event.getEventId(), product.getProductId(), 5, 700000);
        assertTrue(sellerService.removeItemFromOwnEvent(seller, event.getEventId(), newItem.getFlashItemId()));
        assertTrue(itemRepo.findByEvent(event.getEventId()).isEmpty());

        sellerService.addItemToOwnEvent(seller, event.getEventId(), product.getProductId(), 5, 700000);
        assertEquals(SaleStatus.CHO_PHE_DUYET,
                sellerService.resubmitRejectedEvent(seller, event.getEventId()).getStatus());
    }
}
