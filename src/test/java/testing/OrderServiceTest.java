package testing;

import exception.EventNotActiveException;
import exception.ExceedPurchaseLimitException;
import model.Customer;
import model.FlashSaleEvent;
import model.FlashSaleItem;
import model.Order;
import model.OrderDetail;
import model.Product;
import model.Seller;
import model.enums.CustomerTier;
import model.enums.OrderStatus;
import model.enums.ProductCategory;
import model.enums.SaleStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import repository.CustomerRepository;
import repository.FlashSaleEventRepository;
import repository.FlashSaleItemRepository;
import repository.OrderDetailRepository;
import repository.OrderRepository;
import repository.ProductRepository;
import service.BookingResult;
import service.OrderService;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrderServiceTest {

    @AfterEach
    public void cleanup() {
        cleanupSet("success");
        cleanupSet("inactive");
        cleanupSet("limit");
        cleanupSet("tier");
        cleanupSet("cancel");
        cleanupSet("seller");
        cleanupSet("regular");
    }

    @Test
    public void placeOrderNoLockSingleThreadCreatesOrderAndDetail() throws Exception {
        TestRepos repos = createRepos("success");

        BookingResult result = repos.orderService.placeOrderNoLock("CUS-TEST", "FSI-TEST", 2);

        assertEquals("ORD-00001", result.getOrder().getOrderId());
        assertEquals("DTL-00001", result.getOrderDetail().getDetailId());
        assertEquals(2, result.getOrderDetail().getQuantity());
        assertEquals(2, result.getFlashSaleItem().getSoldQty());
        assertEquals(2, result.getFlashSaleItem().getVersion());
        assertEquals(1, repos.orderRepository.count());
        assertEquals(1, repos.orderDetailRepository.count());
        assertEquals(200000.0, result.getOrder().getTotalAmount(), 0.01);
    }

    @Test
    public void placeOrderNoLockRejectsInactiveEvent() throws Exception {
        TestRepos repos = createRepos("inactive");
        FlashSaleEvent event = repos.eventRepository.findById("EVT-TEST").get();
        event.setStatus(SaleStatus.SAP_DIEN_RA);
        repos.eventRepository.update(event);

        assertThrows(EventNotActiveException.class,
                () -> repos.orderService.placeOrderNoLock("CUS-TEST", "FSI-TEST", 1));
    }

    @Test
    public void placeOrderNoLockRejectsPurchaseLimitExceeded() throws Exception {
        TestRepos repos = createRepos("limit");
        repos.orderRepository.save(new Order(
                "ORD-00001", "CUS-TEST", "EVT-TEST",
                "2026-01-01T00:00:00", OrderStatus.DA_XAC_NHAN, 200000.0));
        repos.orderDetailRepository.save(new OrderDetail(
                "DTL-00001", "ORD-00001", "FSI-TEST", 2, 100000.0));

        assertThrows(ExceedPurchaseLimitException.class,
                () -> repos.orderService.placeOrderNoLock("CUS-TEST", "FSI-TEST", 1));
    }

    @Test
    public void placeOrderNoLockAppliesTierDiscountAndUpgradesTier() throws Exception {
        TestRepos repos = createRepos("tier");
        Customer customer = new Customer(
                "CUS-TEST", "Nguyen Van A", "a.nguyen@email.com", CustomerTier.PREMIUM, "2026-01-01");
        repos.customerRepository.save(customer);
        repos.orderRepository.save(new Order(
                "ORD-00001", "CUS-TEST", "EVT-OLD",
                "2026-01-01T00:00:00", OrderStatus.DA_XAC_NHAN, 900000.0));

        BookingResult result = repos.orderService.placeOrderNoLock(customer, "FSI-TEST", 2);

        assertEquals(CustomerTier.PREMIUM, result.getTierBeforeOrder());
        assertEquals(5.0, result.getDiscountPercent(), 0.01);
        assertEquals(10000.0, result.getDiscountAmount(), 0.01);
        assertEquals(190000.0, result.getOrder().getTotalAmount(), 0.01);
        assertEquals(CustomerTier.VIP, result.getTierAfterOrder());
        assertEquals(CustomerTier.VIP, customer.getTier());
    }

    @Test
    public void customerCanViewAndCancelOwnOrderAndStockIsRestored() throws Exception {
        TestRepos repos = createRepos("cancel");
        Customer customer = new Customer(
                "CUS-TEST", "Nguyen Van A", "cancel@email.com", CustomerTier.REGULAR, "2026-01-01");
        repos.customerRepository.save(customer);

        BookingResult result = repos.orderService.placeOrderNoLock(customer, "FSI-TEST", 2);
        assertEquals(1, repos.orderService.getOrdersForCustomer(customer).size());
        assertEquals(2, repos.itemRepository.findById("FSI-TEST").get().getSoldQty());

        Order cancelled = repos.orderService.cancelOrder(customer, result.getOrder().getOrderId());
        assertEquals(OrderStatus.DA_HUY, cancelled.getStatus());
        assertEquals(0, repos.itemRepository.findById("FSI-TEST").get().getSoldQty());
    }

    @Test
    public void sellerCanViewOwnOrdersDetailsAndUpdateStatusInSequence() throws Exception {
        TestRepos repos = createRepos("seller");
        Seller seller = new Seller(
                "SEL-TEST", "Shop Test", "shop@test.com", "hash", "salt",
                Collections.singletonList("PRD-TEST"),
                Collections.singletonList("EVT-TEST"), "2026-01-01");

        BookingResult result = repos.orderService.placeOrderNoLock("CUS-TEST", "FSI-TEST", 1);

        assertEquals(1, repos.orderService.getOrdersForSeller(seller).size());
        assertEquals(1, repos.orderService
                .getOrderDetailsForSeller(seller, result.getOrder().getOrderId()).size());

        Order preparing = repos.orderService.updateOrderStatusForSeller(
                seller, result.getOrder().getOrderId(), OrderStatus.DANG_CHUAN_BI);
        assertEquals(OrderStatus.DANG_CHUAN_BI, preparing.getStatus());

        assertThrows(IllegalArgumentException.class,
                () -> repos.orderService.updateOrderStatusForSeller(
                        seller, result.getOrder().getOrderId(), OrderStatus.HOAN_THANH));

        Order shipping = repos.orderService.updateOrderStatusForSeller(
                seller, result.getOrder().getOrderId(), OrderStatus.DANG_GIAO);
        Order completed = repos.orderService.updateOrderStatusForSeller(
                seller, result.getOrder().getOrderId(), OrderStatus.HOAN_THANH);
        assertEquals(OrderStatus.DANG_GIAO, shipping.getStatus());
        assertEquals(OrderStatus.HOAN_THANH, completed.getStatus());

        Seller anotherSeller = new Seller(
                "SEL-OTHER", "Other Shop", "other@test.com", "hash", "salt",
                Collections.emptyList(), Arrays.asList("EVT-OTHER"), "2026-01-01");
        assertThrows(IllegalArgumentException.class,
                () -> repos.orderService.getOrderForSeller(
                        anotherSeller, result.getOrder().getOrderId()));
    }

    @Test
    public void buyerCanOrderAndCancelRegularProductAndSellerCanSeeIt() throws Exception {
        TestRepos repos = createRepos("regular");
        Customer customer = new Customer(
                "CUS-TEST", "Regular Buyer", "regular@test.com", CustomerTier.REGULAR, "2026-01-01");
        repos.customerRepository.save(customer);
        Seller seller = new Seller(
                "SEL-TEST", "Regular Shop", "shop@test.com", "hash", "salt",
                Collections.singletonList("PRD-TEST"), Collections.emptyList(), "2026-01-01");

        BookingResult result = repos.orderService.placeRegularProductOrder(customer, "PRD-TEST", 2);

        assertEquals("REGULAR", result.getOrder().getEventId());
        assertEquals("PRD-TEST", result.getOrderDetail().getFlashItemId());
        assertEquals(3, repos.productRepository.findById("PRD-TEST").get().getStock());
        assertEquals(1, repos.orderService.getOrdersForSeller(seller).size());

        repos.orderService.cancelOrder(customer, result.getOrder().getOrderId());
        assertEquals(5, repos.productRepository.findById("PRD-TEST").get().getStock());
    }

    private TestRepos createRepos(String suffix) {
        String base = "data/test_order_service_" + suffix + "_";
        delete(base + "events.csv");
        delete(base + "items.csv");
        delete(base + "orders.csv");
        delete(base + "details.csv");
        delete(base + "customers.csv");
        delete(base + "products.csv");

        CustomerRepository customerRepository = new CustomerRepository(base + "customers.csv");
        FlashSaleEventRepository eventRepository = new FlashSaleEventRepository(base + "events.csv");
        FlashSaleItemRepository itemRepository = new FlashSaleItemRepository(base + "items.csv");
        OrderRepository orderRepository = new OrderRepository(base + "orders.csv");
        OrderDetailRepository detailRepository = new OrderDetailRepository(base + "details.csv");
        ProductRepository productRepository = new ProductRepository(base + "products.csv");

        eventRepository.save(new FlashSaleEvent(
                "EVT-TEST", "Test Event", "2026-01-01T00:00:00",
                "2026-01-01T01:00:00", SaleStatus.DANG_DIEN_RA, 50));
        itemRepository.save(new FlashSaleItem(
                "FSI-TEST", "EVT-TEST", "PRD-TEST", 5, 0, 100000.0, 1));
        productRepository.save(new Product(
                "PRD-TEST", "Regular Product", ProductCategory.DIEN_TU, 300000.0, 5, 1));

        OrderService orderService = new OrderService(
                orderRepository, detailRepository, itemRepository, eventRepository,
                customerRepository, productRepository);
        return new TestRepos(customerRepository, eventRepository, itemRepository,
                orderRepository, detailRepository, productRepository, orderService);
    }

    private void cleanupSet(String suffix) {
        String base = "data/test_order_service_" + suffix + "_";
        delete(base + "events.csv");
        delete(base + "items.csv");
        delete(base + "orders.csv");
        delete(base + "details.csv");
        delete(base + "customers.csv");
        delete(base + "products.csv");
    }

    private void delete(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            assertTrue(file.delete());
        }
    }

    private static class TestRepos {
        private final CustomerRepository customerRepository;
        private final FlashSaleEventRepository eventRepository;
        private final FlashSaleItemRepository itemRepository;
        private final OrderRepository orderRepository;
        private final OrderDetailRepository orderDetailRepository;
        private final ProductRepository productRepository;
        private final OrderService orderService;

        private TestRepos(CustomerRepository customerRepository,
                          FlashSaleEventRepository eventRepository,
                          FlashSaleItemRepository itemRepository,
                          OrderRepository orderRepository,
                          OrderDetailRepository orderDetailRepository,
                          ProductRepository productRepository,
                          OrderService orderService) {
            this.customerRepository = customerRepository;
            this.eventRepository = eventRepository;
            this.itemRepository = itemRepository;
            this.orderRepository = orderRepository;
            this.orderDetailRepository = orderDetailRepository;
            this.productRepository = productRepository;
            this.orderService = orderService;
        }
    }
}
