package testing;

import model.*;
import model.enums.*;
import repository.*;
import exception.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RepositoryTest {

    @BeforeAll
    public static void checkData() {
        if (!new File("data/products.csv").exists()) {
            fail("⚠ Chưa có dữ liệu! Hãy chạy DataGenerator trước.");
        }
    }

    // ======================== PRODUCT REPOSITORY ========================
    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("Test ProductRepository")
    public void testProductRepository() {
        ProductRepository repo = new ProductRepository("data/products.csv");

        List<Product> all = repo.findAll();
        assertFalse(all.isEmpty(), "findAll() phải đọc được dữ liệu");
        assertEquals(5000, all.size(), "findAll() đọc đúng 5000 dòng");

        Optional<Product> first = repo.findById("PRD-00001");
        assertTrue(first.isPresent(), "findById('PRD-00001') tìm thấy");
        first.ifPresent(p -> {
            assertEquals("PRD-00001", p.getProductId(), "productId đúng");
            assertNotNull(p.getName(), "có tên sản phẩm");
            assertFalse(p.getName().isEmpty(), "có tên sản phẩm");
            assertTrue(p.getOriginalPrice() > 0, "giá > 0");
        });

        List<Product> dienTu = repo.findByCategory(ProductCategory.DIEN_TU);
        assertFalse(dienTu.isEmpty(), "findByCategory(DIEN_TU) có kết quả");
        assertTrue(dienTu.stream().allMatch(p -> p.getCategory() == ProductCategory.DIEN_TU), "tất cả đều DIEN_TU");

        List<Product> priceRange = repo.findByPriceRange(100_000, 500_000);
        assertFalse(priceRange.isEmpty(), "findByPriceRange có kết quả");
        assertTrue(priceRange.stream().allMatch(p -> p.getOriginalPrice() >= 100_000 && p.getOriginalPrice() <= 500_000), "trong khoảng giá");

        List<Product> searchName = repo.findByName("Tai nghe");
        assertFalse(searchName.isEmpty(), "findByName có kết quả");

        List<Product> inStock = repo.findInStock();
        assertFalse(inStock.isEmpty(), "findInStock có kết quả");
    }

    // ======================== CUSTOMER REPOSITORY ========================
    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("Test CustomerRepository")
    public void testCustomerRepository() {
        CustomerRepository repo = new CustomerRepository("data/customers.csv");

        List<Customer> all = repo.findAll();
        assertFalse(all.isEmpty());
        assertEquals(2000, all.size());

        Optional<Customer> first = repo.findById("CUS-00001");
        assertTrue(first.isPresent());

        List<Customer> vips = repo.findByTier(CustomerTier.VIP);
        assertFalse(vips.isEmpty());
        assertTrue(vips.stream().allMatch(c -> c.getTier() == CustomerTier.VIP));

        if (!all.isEmpty()) {
            String email = all.get(0).getEmail();
            assertTrue(repo.findByEmail(email).isPresent());
        }
    }

    // ======================== FLASH SALE EVENT REPOSITORY ========================
    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("Test FlashSaleEventRepository")
    public void testFlashSaleEventRepository() {
        FlashSaleEventRepository repo = new FlashSaleEventRepository("data/flash_events.csv");

        List<FlashSaleEvent> all = repo.findAll();
        assertFalse(all.isEmpty());
        assertEquals(20, all.size());

        List<FlashSaleEvent> dangDienRa = repo.findDangDienRa();
        assertFalse(dangDienRa.isEmpty());
        assertTrue(dangDienRa.stream().allMatch(e -> e.getStatus() == SaleStatus.DANG_DIEN_RA));

        List<FlashSaleEvent> daKetThuc = repo.findDaKetThuc();
        assertFalse(daKetThuc.isEmpty());
    }

    // ======================== FLASH SALE ITEM REPOSITORY ========================
    @Test
    @org.junit.jupiter.api.Order(4)
    @DisplayName("Test FlashSaleItemRepository")
    public void testFlashSaleItemRepository() {
        FlashSaleItemRepository repo = new FlashSaleItemRepository("data/flash_items.csv");

        List<FlashSaleItem> all = repo.findAll();
        assertFalse(all.isEmpty());
        assertEquals(500, all.size());

        Optional<FlashSaleItem> first = repo.findById("FSI-00001");
        assertTrue(first.isPresent());
        first.ifPresent(item -> {
            assertEquals("FSI-00001", item.getFlashItemId());
            assertTrue(item.getLimitedQty() > 0);
            assertTrue(item.getSoldQty() >= 0);
            assertTrue(item.getSoldQty() <= item.getLimitedQty());
        });

        if (!all.isEmpty()) {
            assertFalse(repo.findByEvent(all.get(0).getEventId()).isEmpty());
        }

        List<FlashSaleItem> available = repo.findAvailable();
        assertFalse(available.isEmpty());
        assertTrue(available.stream().allMatch(item -> item.soLuongConLai() > 0));
    }

    // ======================== ORDER REPOSITORY ========================
    @Test
    @org.junit.jupiter.api.Order(5)
    @DisplayName("Test OrderRepository")
    public void testOrderRepository() {
        OrderRepository repo = new OrderRepository("data/orders.csv");

        List<Order> all = repo.findAll();
        assertFalse(all.isEmpty());
        assertEquals(2500, all.size());

        if (!all.isEmpty()) {
            assertFalse(repo.findByCustomer(all.get(0).getCustomerId()).isEmpty());
        }

        assertFalse(repo.findByStatus(OrderStatus.CHO_XU_LY).isEmpty());
    }

    // ======================== ORDER DETAIL REPOSITORY ========================
    @Test
    @org.junit.jupiter.api.Order(6)
    @DisplayName("Test OrderDetailRepository")
    public void testOrderDetailRepository() {
        OrderDetailRepository repo = new OrderDetailRepository("data/order_details.csv");

        List<OrderDetail> all = repo.findAll();
        assertFalse(all.isEmpty());
        assertEquals(2500, all.size());

        if (!all.isEmpty()) {
            String orderId = all.get(0).getOrderId();
            assertFalse(repo.findByOrder(orderId).isEmpty());
            assertTrue(repo.tinhTongTien(orderId) > 0);
        }
    }

    // ======================== ORDER TRANSACTION REPOSITORY ========================
    @Test
    @org.junit.jupiter.api.Order(7)
    @DisplayName("Test OrderTransactionRepository (CRUD trên file tạm)")
    public void testOrderTransactionRepository() {
        String testFile = "data/test_transactions.csv";
        OrderTransactionRepository repo = new OrderTransactionRepository(testFile);

        OrderTransaction txn = new OrderTransaction(
                "TXN-TEST-001", "ORD-00001", LockMechanism.SYNCHRONIZED,
                "TestThread-1", 1000000L, 2000000L, true, "");
        repo.save(txn);
        assertTrue(repo.count() >= 1);

        Optional<OrderTransaction> found = repo.findById("TXN-TEST-001");
        assertTrue(found.isPresent());
        found.ifPresent(t -> {
            assertEquals("ORD-00001", t.getOrderId());
            assertEquals(LockMechanism.SYNCHRONIZED, t.getMechanism());
            assertTrue(t.isSuccess());
        });

        assertFalse(repo.findByMechanism(LockMechanism.SYNCHRONIZED).isEmpty());

        OrderTransaction updated = new OrderTransaction(
                "TXN-TEST-001", "ORD-00001", LockMechanism.SYNCHRONIZED,
                "TestThread-1", 1000000L, 3000000L, false, "Test error");
        repo.update(updated);
        Optional<OrderTransaction> afterUpdate = repo.findById("TXN-TEST-001");
        assertTrue(afterUpdate.isPresent());
        assertEquals(3000000L, afterUpdate.get().getEndTime());
        assertFalse(afterUpdate.get().isSuccess());

        assertTrue(repo.deleteById("TXN-TEST-001"));
        assertFalse(repo.findById("TXN-TEST-001").isPresent());

        repo.save(txn);
        repo.clearAll();
        assertEquals(0L, repo.count());

        new File(testFile).delete();
    }

    // ======================== PERFORMANCE TEST ========================
    @Test
    @org.junit.jupiter.api.Order(8)
    @DisplayName("Test Performance Đọc 10k dòng")
    public void testPerformance() {
        // Đánh dấu thời gian bắt đầu cho CẢ QUÁ TRÌNH
        long start = System.currentTimeMillis();

        // Đọc liên tục 3 kho dữ liệu
        List<Product> products = new ProductRepository("data/products.csv").findAll();
        List<Customer> customers = new CustomerRepository("data/customers.csv").findAll();
        List<Order> orders = new OrderRepository("data/orders.csv").findAll();

        // Tính tổng thời gian đã trôi qua
        long totalElapsed = System.currentTimeMillis() - start;

        // Tính tổng số lượng dòng
        int totalLines = products.size() + customers.size() + orders.size();

        // Khẳng định tổng số dòng >= 9500 (Gần 10k)
        assertTrue(totalLines >= 9500, "Tổng đọc >= 9500 dòng");
        
        // CHUẨN NHẤT: Khẳng định TỔNG thời gian của cả 3 thao tác phải < 1 giây
        assertTrue(totalElapsed < 1000, "Tổng thời gian đọc " + totalLines + " dòng là " + totalElapsed + "ms (Phải < 1000ms)");
    }

    // ======================== 4 LOCK MECHANISMS TEST ========================
    @Test
    @org.junit.jupiter.api.Order(9)
    @DisplayName("Test 4 Lock Mechanisms")
    public void testLockMechanisms() {
        String testFile = "data/test_flash_items.csv";
        copyFile("data/flash_items.csv", testFile);
        FlashSaleItemRepository repo = new FlashSaleItemRepository(testFile);

        List<FlashSaleItem> all = repo.findAll();
        if (all.isEmpty()) return;

        FlashSaleItem testItem = all.get(0);
        String itemId = testItem.getFlashItemId();

        // 1. NO_LOCK
        repo.resetAllSoldQty();
        assertDoesNotThrow(() -> repo.sellNoLock(itemId, 1));
        FlashSaleItem after1 = repo.findById(itemId).orElse(null);
        assertNotNull(after1);
        assertEquals(1, after1.getSoldQty());
        assertEquals(2, after1.getVersion());

        // OutOfStock test
        repo.resetAllSoldQty();
        FlashSaleItem item = repo.findById(itemId).orElse(null);
        if (item != null) {
            item.setSoldQty(item.getLimitedQty());
            item.setVersion(item.getVersion() + 1);
            repo.update(item);
        }
        assertThrows(OutOfStockException.class, () -> repo.sellNoLock(itemId, 1));

        // 2. SYNCHRONIZED
        repo.resetAllSoldQty();
        assertDoesNotThrow(() -> repo.sellWithSynchronized(itemId, 1));
        FlashSaleItem after2 = repo.findById(itemId).orElse(null);
        assertNotNull(after2);
        assertEquals(1, after2.getSoldQty());
        assertEquals(2, after2.getVersion());

        // 3. FILE_LOCK
        repo.resetAllSoldQty();
        assertDoesNotThrow(() -> repo.sellWithFileLock(itemId, 1));
        FlashSaleItem after3 = repo.findById(itemId).orElse(null);
        assertNotNull(after3);
        assertEquals(1, after3.getSoldQty());
        assertEquals(2, after3.getVersion());

        // 4. OPTIMISTIC LOCK
        repo.resetAllSoldQty();
        assertDoesNotThrow(() -> repo.sellWithOptimisticLock(itemId, 1));
        FlashSaleItem after4 = repo.findById(itemId).orElse(null);
        assertNotNull(after4);
        assertEquals(1, after4.getSoldQty());
        assertEquals(2, after4.getVersion());

        // Invariant check
        repo.resetAllSoldQty();
        List<FlashSaleItem> finalAll = repo.findAll();
        assertTrue(finalAll.stream().allMatch(i -> i.getSoldQty() <= i.getLimitedQty()));

        new File(testFile).delete();
    }

    private void copyFile(String src, String dest) {
        try {
            Files.copy(Paths.get(src), Paths.get(dest), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            System.err.println("Lỗi copy file: " + e.getMessage());
        }
    }
}
