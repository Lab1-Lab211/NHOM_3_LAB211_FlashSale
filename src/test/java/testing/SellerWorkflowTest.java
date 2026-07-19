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
    void sellerRegistersAndCreatesUpcomingFlashSaleWithoutAdminApproval() {
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
                seller, "Sale cua Shop A", "2999-01-01T08:00:00", "2999-01-01T10:00:00", 30);
        FlashSaleItem item = sellerService.addItemToOwnEvent(
                seller, event.getEventId(), product.getProductId(), 5, 140000);

        assertTrue(seller.ownsProduct(product.getProductId()));
        assertTrue(seller.ownsEvent(event.getEventId()));
        assertEquals(SaleStatus.SAP_DIEN_RA, event.getStatus());
        assertEquals(5, item.getLimitedQty());
    }

    @Test
    void upcomingFlashSaleCanBeEditedAndItsItemsCanBeChanged() {
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
                seller, "Sale lan dau", "2999-02-01T08:00:00", "2999-02-01T10:00:00", 20);
        sellerService.addItemToOwnEvent(seller, event.getEventId(), product.getProductId(), 5, 800000);

        FlashSaleEvent updated = sellerService.updateOwnEvent(
                seller, event.getEventId(), "Sale da chinh sua", "", "", 30);
        assertEquals("Sale da chinh sua", updated.getEventName());

        FlashSaleItem newItem = sellerService.addItemToOwnEvent(
                seller, event.getEventId(), product.getProductId(), 5, 700000);
        assertTrue(sellerService.removeItemFromOwnEvent(seller, event.getEventId(), newItem.getFlashItemId()));
        assertTrue(itemRepo.findByEvent(event.getEventId()).isEmpty());

        sellerService.addItemToOwnEvent(seller, event.getEventId(), product.getProductId(), 5, 700000);
        assertEquals(SaleStatus.SAP_DIEN_RA,
                eventRepo.findById(event.getEventId()).get().getStatus());
    }

    @Test
    void flashSaleStartsAutomaticallyAtStartTimeAndOnlyThenCanEnd() {
        FlashSaleEventRepository eventRepo = new FlashSaleEventRepository(EVENTS);
        FlashSaleItemRepository itemRepo = new FlashSaleItemRepository(ITEMS);
        ProductRepository productRepo = new ProductRepository(PRODUCTS);
        FlashSaleItemService itemService = new FlashSaleItemService(itemRepo, eventRepo, productRepo);
        FlashSaleService flashSaleService = new FlashSaleService(eventRepo, itemService);

        FlashSaleEvent futureEvent = new FlashSaleEvent(
                "EVT-FUTURE", "Future Event", "2999-03-01T08:00:00", "2999-03-01T10:00:00",
                SaleStatus.SAP_DIEN_RA, 20);
        eventRepo.save(futureEvent);

        assertThrows(IllegalArgumentException.class,
                () -> flashSaleService.endEvent(futureEvent.getEventId()));

        FlashSaleEvent startedEvent = new FlashSaleEvent(
                "EVT-STARTED", "Started Event", "2000-03-01T08:00:00", "2999-03-01T10:00:00",
                SaleStatus.SAP_DIEN_RA, 20);
        eventRepo.save(startedEvent);

        assertEquals(SaleStatus.DANG_DIEN_RA,
                eventRepo.findById(startedEvent.getEventId()).get().getStatus());
        assertEquals(SaleStatus.DA_KET_THUC,
                flashSaleService.endEvent(startedEvent.getEventId()).get().getStatus());
    }
}
