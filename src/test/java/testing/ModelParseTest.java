package testing;

import model.*;
import model.enums.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 — Unit Test cho tất cả Entity (Model Layer).
 * Kiểm tra parse/serialize CSV round-trip, nghiệp vụ, enum, BaseEntity.
 *
 * Yêu cầu T3: "Unit test parse CSV đúng" — Test parse/serialize pass 100%.
 */
@DisplayName("Model Layer — Parse/Serialize CSV")
public class ModelParseTest {

    // ======================== PRODUCT ========================

    @Nested
    @DisplayName("📦 Product")
    class ProductTest {

        @Test
        @DisplayName("Round-trip: toCsvLine → fromCsvLine → toCsvLine khớp nhau")
        void roundTrip() {
            Product p = new Product("PRD-00001", "Tai nghe Bluetooth DT-0001",
                    ProductCategory.DIEN_TU, 500000.0, 100, 1);
            String csv = p.toCsvLine();
            Product p2 = new Product();
            p2.fromCsvLine(csv);
            assertEquals(csv, p2.toCsvLine());
        }

        @Test
        @DisplayName("Parse đúng tất cả trường")
        void parseFields() {
            Product p = new Product("PRD-00001", "Tai nghe Bluetooth DT-0001",
                    ProductCategory.DIEN_TU, 500000.0, 100, 1);
            Product p2 = new Product();
            p2.fromCsvLine(p.toCsvLine());

            assertEquals("PRD-00001", p2.getProductId());
            assertEquals("Tai nghe Bluetooth DT-0001", p2.getName());
            assertEquals(ProductCategory.DIEN_TU, p2.getCategory());
            assertEquals(500000.0, p2.getOriginalPrice(), 0.01);
            assertEquals(100, p2.getStock());
            assertEquals(1, p2.getVersion());
        }

        @Test
        @DisplayName("ID mapping: id == productId")
        void idMapping() {
            Product p = new Product();
            p.fromCsvLine("PRD-00001,Test,DIEN_TU,100000.0,10,1");
            assertEquals(p.getProductId(), p.getId());
        }

        @Test
        @DisplayName("getCsvHeader() đúng format")
        void csvHeader() {
            assertEquals("productId,name,category,originalPrice,stock,version",
                    new Product().getCsvHeader());
        }

        @Test
        @DisplayName("Round-trip giá trị lớn")
        void roundTripLargeValues() {
            Product p = new Product("PRD-99999", "Máy tính bảng DT-9999",
                    ProductCategory.DIEN_TU, 4999000.0, 999, 50);
            String csv = p.toCsvLine();
            Product p2 = new Product();
            p2.fromCsvLine(csv);
            assertEquals(csv, p2.toCsvLine());
        }

        @Test
        @DisplayName("Round-trip tất cả ProductCategory")
        void allCategories() {
            for (ProductCategory cat : ProductCategory.values()) {
                Product p = new Product("P-" + cat.name(), "Test " + cat.name(),
                        cat, 100000.0, 10, 1);
                Product p2 = new Product();
                p2.fromCsvLine(p.toCsvLine());
                assertEquals(cat, p2.getCategory(), "Category round-trip: " + cat.name());
            }
        }

        @Test
        @DisplayName("toString() không ném exception")
        void toStringNoException() {
            Product p = new Product("PRD-00001", "Test", ProductCategory.DIEN_TU, 500000, 100, 1);
            assertDoesNotThrow(() -> p.toString());
        }

        @Test
        @DisplayName("setProductId() cập nhật cả id")
        void setProductIdUpdatesId() {
            Product p = new Product();
            p.setProductId("PRD-SET");
            assertEquals("PRD-SET", p.getId());
        }
    }

    // ======================== CUSTOMER ========================

    @Nested
    @DisplayName("📦 Customer")
    class CustomerTest {

        @Test
        @DisplayName("Round-trip: toCsvLine → fromCsvLine → toCsvLine khớp nhau")
        void roundTrip() {
            Customer c = new Customer("CUS-00001", "Nguyễn Văn An",
                    "an.nguyen1@email.com", CustomerTier.VIP, "2024-05-15");
            String csv = c.toCsvLine();
            Customer c2 = new Customer();
            c2.fromCsvLine(csv);
            assertEquals(csv, c2.toCsvLine());
        }

        @Test
        @DisplayName("Parse đúng tất cả trường")
        void parseFields() {
            Customer c = new Customer("CUS-00001", "Nguyễn Văn An",
                    "an.nguyen1@email.com", CustomerTier.VIP, "2024-05-15");
            Customer c2 = new Customer();
            c2.fromCsvLine(c.toCsvLine());

            assertEquals("CUS-00001", c2.getCustomerId());
            assertEquals("Nguyễn Văn An", c2.getName());
            assertEquals("an.nguyen1@email.com", c2.getEmail());
            assertEquals(CustomerTier.VIP, c2.getTier());
            assertEquals("2024-05-15", c2.getRegisteredDate());
        }

        @Test
        @DisplayName("ID mapping: id == customerId")
        void idMapping() {
            Customer c = new Customer("CUS-00001", "Test", "t@t.com",
                    CustomerTier.REGULAR, "2025-01-01");
            assertEquals(c.getCustomerId(), c.getId());
        }

        @Test
        @DisplayName("getCsvHeader() đúng format")
        void csvHeader() {
            assertEquals("customerId,name,email,tier,registeredDate",
                    new Customer().getCsvHeader());
        }

        @Test
        @DisplayName("Round-trip tất cả CustomerTier")
        void allTiers() {
            for (CustomerTier tier : CustomerTier.values()) {
                Customer c = new Customer("C-" + tier.name(), "Test", "t@t.com",
                        tier, "2025-01-01");
                Customer c2 = new Customer();
                c2.fromCsvLine(c.toCsvLine());
                assertEquals(tier, c2.getTier(), "Tier round-trip: " + tier.name());
            }
        }

        @Test
        @DisplayName("Tên tiếng Việt có dấu round-trip đúng")
        void vietnameseName() {
            Customer c = new Customer("CUS-00002", "Trần Thị Phương Thảo",
                    "thao.tran@email.com", CustomerTier.PREMIUM, "2025-03-20");
            Customer c2 = new Customer();
            c2.fromCsvLine(c.toCsvLine());
            assertEquals("Trần Thị Phương Thảo", c2.getName());
        }

        @Test
        @DisplayName("toString() không ném exception")
        void toStringNoException() {
            Customer c = new Customer("CUS-00001", "Test", "t@t.com",
                    CustomerTier.VIP, "2025-01-01");
            assertDoesNotThrow(() -> c.toString());
        }
    }

    // ======================== FLASH SALE EVENT ========================

    @Nested
    @DisplayName("📦 FlashSaleEvent")
    class FlashSaleEventTest {

        @Test
        @DisplayName("Round-trip: toCsvLine → fromCsvLine → toCsvLine khớp nhau")
        void roundTrip() {
            FlashSaleEvent e = new FlashSaleEvent("EVT-001", "Flash Sale Mùa Hè #1",
                    "2026-06-15T10:00:00", "2026-06-15T12:00:00",
                    SaleStatus.DANG_DIEN_RA, 50);
            String csv = e.toCsvLine();
            FlashSaleEvent e2 = new FlashSaleEvent();
            e2.fromCsvLine(csv);
            assertEquals(csv, e2.toCsvLine());
        }

        @Test
        @DisplayName("Parse đúng tất cả trường")
        void parseFields() {
            FlashSaleEvent e = new FlashSaleEvent("EVT-001", "Flash Sale Mùa Hè #1",
                    "2026-06-15T10:00:00", "2026-06-15T12:00:00",
                    SaleStatus.DANG_DIEN_RA, 50);
            FlashSaleEvent e2 = new FlashSaleEvent();
            e2.fromCsvLine(e.toCsvLine());

            assertEquals("EVT-001", e2.getEventId());
            assertEquals("Flash Sale Mùa Hè #1", e2.getEventName());
            assertEquals("2026-06-15T10:00:00", e2.getStartTime());
            assertEquals("2026-06-15T12:00:00", e2.getEndTime());
            assertEquals(SaleStatus.DANG_DIEN_RA, e2.getStatus());
            assertEquals(50, e2.getDiscountPercent());
        }

        @Test
        @DisplayName("ID mapping: id == eventId")
        void idMapping() {
            FlashSaleEvent e = new FlashSaleEvent("EVT-001", "Test",
                    "2026-01-01T00:00:00", "2026-01-01T02:00:00",
                    SaleStatus.SAP_DIEN_RA, 30);
            assertEquals(e.getEventId(), e.getId());
        }

        @Test
        @DisplayName("getCsvHeader() đúng format")
        void csvHeader() {
            assertEquals("eventId,eventName,startTime,endTime,status,discountPercent",
                    new FlashSaleEvent().getCsvHeader());
        }

        @Test
        @DisplayName("Round-trip tất cả SaleStatus")
        void allStatuses() {
            for (SaleStatus status : SaleStatus.values()) {
                FlashSaleEvent e = new FlashSaleEvent("E-" + status.name(), "Test",
                        "2026-01-01T00:00:00", "2026-01-01T02:00:00", status, 30);
                FlashSaleEvent e2 = new FlashSaleEvent();
                e2.fromCsvLine(e.toCsvLine());
                assertEquals(status, e2.getStatus(), "Status round-trip: " + status.name());
            }
        }

        @Test
        @DisplayName("Biên giảm giá: 30% và 70%")
        void discountBounds() {
            FlashSaleEvent eLow = new FlashSaleEvent("EVT-L", "Sale30",
                    "2026-01-01T00:00:00", "2026-01-01T02:00:00", SaleStatus.SAP_DIEN_RA, 30);
            FlashSaleEvent eLow2 = new FlashSaleEvent();
            eLow2.fromCsvLine(eLow.toCsvLine());
            assertEquals(30, eLow2.getDiscountPercent());

            FlashSaleEvent eHigh = new FlashSaleEvent("EVT-H", "Sale70",
                    "2026-01-01T00:00:00", "2026-01-01T02:00:00", SaleStatus.DA_KET_THUC, 70);
            FlashSaleEvent eHigh2 = new FlashSaleEvent();
            eHigh2.fromCsvLine(eHigh.toCsvLine());
            assertEquals(70, eHigh2.getDiscountPercent());
        }

        @Test
        @DisplayName("toString() không ném exception")
        void toStringNoException() {
            FlashSaleEvent e = new FlashSaleEvent("EVT-001", "Test",
                    "2026-01-01T00:00:00", "2026-01-01T02:00:00", SaleStatus.DANG_DIEN_RA, 50);
            assertDoesNotThrow(() -> e.toString());
        }
    }

    // ======================== FLASH SALE ITEM (CRITICAL) ========================

    @Nested
    @DisplayName("📦 FlashSaleItem (CRITICAL)")
    class FlashSaleItemTest {

        @Test
        @DisplayName("Round-trip: toCsvLine → fromCsvLine → toCsvLine khớp nhau")
        void roundTrip() {
            FlashSaleItem fi = new FlashSaleItem("FSI-00001", "EVT-001", "PRD-00001",
                    50, 10, 250000.0, 3);
            String csv = fi.toCsvLine();
            FlashSaleItem fi2 = new FlashSaleItem();
            fi2.fromCsvLine(csv);
            assertEquals(csv, fi2.toCsvLine());
        }

        @Test
        @DisplayName("Parse đúng tất cả trường")
        void parseFields() {
            FlashSaleItem fi = new FlashSaleItem("FSI-00001", "EVT-001", "PRD-00001",
                    50, 10, 250000.0, 3);
            FlashSaleItem fi2 = new FlashSaleItem();
            fi2.fromCsvLine(fi.toCsvLine());

            assertEquals("FSI-00001", fi2.getFlashItemId());
            assertEquals("EVT-001", fi2.getEventId());
            assertEquals("PRD-00001", fi2.getProductId());
            assertEquals(50, fi2.getLimitedQty());
            assertEquals(10, fi2.getSoldQty());
            assertEquals(250000.0, fi2.getFlashPrice(), 0.01);
            assertEquals(3, fi2.getVersion());
        }

        @Test
        @DisplayName("ID mapping: id == flashItemId")
        void idMapping() {
            FlashSaleItem fi = new FlashSaleItem("FSI-00001", "EVT-001", "PRD-00001",
                    50, 10, 250000.0, 3);
            assertEquals(fi.getFlashItemId(), fi.getId());
        }

        @Test
        @DisplayName("getCsvHeader() đúng format")
        void csvHeader() {
            assertEquals("flashItemId,eventId,productId,limitedQty,soldQty,flashPrice,version",
                    new FlashSaleItem().getCsvHeader());
        }

        @Test
        @DisplayName("coBanDuoc(): soldQty=8, limit=10, mua 2 → true")
        void coBanDuoc_vua_du() {
            FlashSaleItem fi = new FlashSaleItem("FSI-TEST", "EVT-001", "PRD-001",
                    10, 8, 100000.0, 1);
            assertTrue(fi.coBanDuoc(2)); // 8+2=10 <= 10
        }

        @Test
        @DisplayName("coBanDuoc(): soldQty=8, limit=10, mua 3 → false")
        void coBanDuoc_vuot_gioi_han() {
            FlashSaleItem fi = new FlashSaleItem("FSI-TEST", "EVT-001", "PRD-001",
                    10, 8, 100000.0, 1);
            assertFalse(fi.coBanDuoc(3)); // 8+3=11 > 10
        }

        @Test
        @DisplayName("coBanDuoc(): soldQty=8, limit=10, mua 1 → true")
        void coBanDuoc_con_hang() {
            FlashSaleItem fi = new FlashSaleItem("FSI-TEST", "EVT-001", "PRD-001",
                    10, 8, 100000.0, 1);
            assertTrue(fi.coBanDuoc(1)); // 8+1=9 <= 10
        }

        @Test
        @DisplayName("soLuongConLai() = limitedQty - soldQty")
        void soLuongConLai() {
            FlashSaleItem fi = new FlashSaleItem("FSI-TEST", "EVT-001", "PRD-001",
                    10, 8, 100000.0, 1);
            assertEquals(2, fi.soLuongConLai()); // 10 - 8 = 2
        }

        @Test
        @DisplayName("Edge case: đã bán hết → coBanDuoc(1) = false, conLai = 0")
        void soldOut() {
            FlashSaleItem fi = new FlashSaleItem("FSI-FULL", "EVT-001", "PRD-001",
                    5, 5, 100000.0, 1);
            assertFalse(fi.coBanDuoc(1));
            assertEquals(0, fi.soLuongConLai());
        }

        @Test
        @DisplayName("Edge case: chưa bán gì → coBanDuoc(100) = true, conLai = 100")
        void nothingSold() {
            FlashSaleItem fi = new FlashSaleItem("FSI-EMPTY", "EVT-001", "PRD-001",
                    100, 0, 500000.0, 1);
            assertTrue(fi.coBanDuoc(100));
            assertEquals(100, fi.soLuongConLai());
        }

        @Test
        @DisplayName("toString() không ném exception")
        void toStringNoException() {
            FlashSaleItem fi = new FlashSaleItem("FSI-00001", "EVT-001", "PRD-00001",
                    50, 10, 250000.0, 3);
            assertDoesNotThrow(() -> fi.toString());
        }
    }

    // ======================== ORDER ========================

    @Nested
    @DisplayName("📦 Order")
    class OrderTest {

        @Test
        @DisplayName("Round-trip: toCsvLine → fromCsvLine → toCsvLine khớp nhau")
        void roundTrip() {
            Order o = new Order("ORD-00001", "CUS-00001", "EVT-001",
                    "2026-06-15T10:05:30", OrderStatus.DA_XAC_NHAN, 750000.0);
            String csv = o.toCsvLine();
            Order o2 = new Order();
            o2.fromCsvLine(csv);
            assertEquals(csv, o2.toCsvLine());
        }

        @Test
        @DisplayName("Parse đúng tất cả trường")
        void parseFields() {
            Order o = new Order("ORD-00001", "CUS-00001", "EVT-001",
                    "2026-06-15T10:05:30", OrderStatus.DA_XAC_NHAN, 750000.0);
            Order o2 = new Order();
            o2.fromCsvLine(o.toCsvLine());

            assertEquals("ORD-00001", o2.getOrderId());
            assertEquals("CUS-00001", o2.getCustomerId());
            assertEquals("EVT-001", o2.getEventId());
            assertEquals("2026-06-15T10:05:30", o2.getOrderTime());
            assertEquals(OrderStatus.DA_XAC_NHAN, o2.getStatus());
            assertEquals(750000.0, o2.getTotalAmount(), 0.01);
        }

        @Test
        @DisplayName("ID mapping: id == orderId")
        void idMapping() {
            Order o = new Order("ORD-00001", "CUS-001", "EVT-001",
                    "2026-01-01T00:00:00", OrderStatus.CHO_XU_LY, 100000);
            assertEquals(o.getOrderId(), o.getId());
        }

        @Test
        @DisplayName("getCsvHeader() đúng format")
        void csvHeader() {
            assertEquals("orderId,customerId,eventId,orderTime,status,totalAmount",
                    new Order().getCsvHeader());
        }

        @Test
        @DisplayName("Round-trip tất cả OrderStatus")
        void allStatuses() {
            for (OrderStatus status : OrderStatus.values()) {
                Order o = new Order("O-" + status.name(), "CUS-001", "EVT-001",
                        "2026-01-01T00:00:00", status, 100000.0);
                Order o2 = new Order();
                o2.fromCsvLine(o.toCsvLine());
                assertEquals(status, o2.getStatus(), "Status round-trip: " + status.name());
            }
        }

        @Test
        @DisplayName("toString() không ném exception")
        void toStringNoException() {
            Order o = new Order("ORD-00001", "CUS-001", "EVT-001",
                    "2026-01-01T00:00:00", OrderStatus.CHO_XU_LY, 100000);
            assertDoesNotThrow(() -> o.toString());
        }
    }

    // ======================== ORDER DETAIL ========================

    @Nested
    @DisplayName("📦 OrderDetail")
    class OrderDetailTest {

        @Test
        @DisplayName("Round-trip: toCsvLine → fromCsvLine → toCsvLine khớp nhau")
        void roundTrip() {
            OrderDetail d = new OrderDetail("DTL-00001", "ORD-00001", "FSI-00001", 2, 250000.0);
            String csv = d.toCsvLine();
            OrderDetail d2 = new OrderDetail();
            d2.fromCsvLine(csv);
            assertEquals(csv, d2.toCsvLine());
        }

        @Test
        @DisplayName("Parse đúng tất cả trường")
        void parseFields() {
            OrderDetail d = new OrderDetail("DTL-00001", "ORD-00001", "FSI-00001", 2, 250000.0);
            OrderDetail d2 = new OrderDetail();
            d2.fromCsvLine(d.toCsvLine());

            assertEquals("DTL-00001", d2.getDetailId());
            assertEquals("ORD-00001", d2.getOrderId());
            assertEquals("FSI-00001", d2.getFlashItemId());
            assertEquals(2, d2.getQuantity());
            assertEquals(250000.0, d2.getUnitPrice(), 0.01);
        }

        @Test
        @DisplayName("ID mapping: id == detailId")
        void idMapping() {
            OrderDetail d = new OrderDetail("DTL-00001", "ORD-001", "FSI-001", 1, 100000);
            assertEquals(d.getDetailId(), d.getId());
        }

        @Test
        @DisplayName("getCsvHeader() đúng format")
        void csvHeader() {
            assertEquals("detailId,orderId,flashItemId,quantity,unitPrice",
                    new OrderDetail().getCsvHeader());
        }

        @Test
        @DisplayName("thanhTien() = quantity × unitPrice (qty=2)")
        void thanhTien() {
            OrderDetail d = new OrderDetail("DTL-00001", "ORD-00001", "FSI-00001", 2, 250000.0);
            assertEquals(500000.0, d.thanhTien(), 0.01); // 2 × 250000
        }

        @Test
        @DisplayName("thanhTien() = quantity × unitPrice (qty=1)")
        void thanhTienQty1() {
            OrderDetail d = new OrderDetail("DTL-00002", "ORD-00002", "FSI-00002", 1, 1500000.0);
            assertEquals(1500000.0, d.thanhTien(), 0.01);
        }

        @Test
        @DisplayName("toString() không ném exception")
        void toStringNoException() {
            OrderDetail d = new OrderDetail("DTL-00001", "ORD-001", "FSI-001", 1, 100000);
            assertDoesNotThrow(() -> d.toString());
        }
    }

    // ======================== ORDER TRANSACTION ========================

    @Nested
    @DisplayName("📦 OrderTransaction")
    class OrderTransactionTest {

        @Test
        @DisplayName("Round-trip: giao dịch thành công")
        void roundTripSuccess() {
            OrderTransaction t = new OrderTransaction("TXN-00001", "ORD-00001",
                    LockMechanism.OPTIMISTIC, "Thread-1",
                    1000000000L, 1005000000L, true, "");
            String csv = t.toCsvLine();
            OrderTransaction t2 = new OrderTransaction();
            t2.fromCsvLine(csv);
            assertEquals(csv, t2.toCsvLine());
        }

        @Test
        @DisplayName("Parse đúng tất cả trường (thành công)")
        void parseFieldsSuccess() {
            OrderTransaction t = new OrderTransaction("TXN-00001", "ORD-00001",
                    LockMechanism.OPTIMISTIC, "Thread-1",
                    1000000000L, 1005000000L, true, "");
            OrderTransaction t2 = new OrderTransaction();
            t2.fromCsvLine(t.toCsvLine());

            assertEquals("TXN-00001", t2.getTransactionId());
            assertEquals("ORD-00001", t2.getOrderId());
            assertEquals(LockMechanism.OPTIMISTIC, t2.getMechanism());
            assertEquals("Thread-1", t2.getThreadName());
            assertEquals(1000000000L, t2.getStartTime());
            assertEquals(1005000000L, t2.getEndTime());
            assertTrue(t2.isSuccess());
        }

        @Test
        @DisplayName("Round-trip: giao dịch thất bại + error message")
        void roundTripFail() {
            OrderTransaction t = new OrderTransaction("TXN-00002", "ORD-00002",
                    LockMechanism.NO_LOCK, "Thread-5",
                    2000000000L, 2001000000L, false, "Hết hàng");
            OrderTransaction t2 = new OrderTransaction();
            t2.fromCsvLine(t.toCsvLine());

            assertEquals(t.toCsvLine(), t2.toCsvLine());
            assertFalse(t2.isSuccess());
            assertEquals("Hết hàng", t2.getErrorMessage());
        }

        @Test
        @DisplayName("ID mapping: id == transactionId")
        void idMapping() {
            OrderTransaction t = new OrderTransaction("TXN-00001", "ORD-001",
                    LockMechanism.SYNCHRONIZED, "Thread-1", 0L, 1000000L, true, "");
            assertEquals(t.getTransactionId(), t.getId());
        }

        @Test
        @DisplayName("getCsvHeader() đúng format")
        void csvHeader() {
            assertEquals("transactionId,orderId,mechanism,threadName,startTime,endTime,success,errorMessage",
                    new OrderTransaction().getCsvHeader());
        }

        @Test
        @DisplayName("thoiGianXuLyMs() tính đúng")
        void thoiGianXuLyMs() {
            OrderTransaction t = new OrderTransaction("TXN-00001", "ORD-00001",
                    LockMechanism.OPTIMISTIC, "Thread-1",
                    1000000000L, 1005000000L, true, "");
            // (1005000000 - 1000000000) / 1_000_000.0 = 5.0 ms
            assertEquals(5.0, t.thoiGianXuLyMs(), 0.01);
        }

        @Test
        @DisplayName("Round-trip tất cả LockMechanism")
        void allMechanisms() {
            for (LockMechanism mech : LockMechanism.values()) {
                OrderTransaction t = new OrderTransaction("T-" + mech.name(), "ORD-001",
                        mech, "Thread-X", 0L, 1000000L, true, "");
                OrderTransaction t2 = new OrderTransaction();
                t2.fromCsvLine(t.toCsvLine());
                assertEquals(mech, t2.getMechanism(), "Mechanism round-trip: " + mech.name());
            }
        }

        @Test
        @DisplayName("toString() không ném exception")
        void toStringNoException() {
            OrderTransaction t = new OrderTransaction("TXN-00001", "ORD-001",
                    LockMechanism.SYNCHRONIZED, "Thread-1", 0L, 1000000L, true, "");
            assertDoesNotThrow(() -> t.toString());
        }
    }

    // ======================== ENUMS ========================

    @Nested
    @DisplayName("📦 Enums")
    class EnumTest {

        @Test
        @DisplayName("ProductCategory: có đúng 6 giá trị")
        void productCategoryCount() {
            assertEquals(6, ProductCategory.values().length);
        }

        @Test
        @DisplayName("ProductCategory: getMoTa() không null")
        void productCategoryMoTa() {
            for (ProductCategory cat : ProductCategory.values()) {
                assertNotNull(cat.getMoTa(), cat.name() + ".getMoTa() không được null");
            }
        }

        @Test
        @DisplayName("CustomerTier: có đúng 3 giá trị")
        void customerTierCount() {
            assertEquals(3, CustomerTier.values().length);
        }

        @Test
        @DisplayName("CustomerTier: VIP ưu tiên > PREMIUM > REGULAR")
        void customerTierPriority() {
            assertTrue(CustomerTier.VIP.getDoUuTien() < CustomerTier.PREMIUM.getDoUuTien());
            assertTrue(CustomerTier.PREMIUM.getDoUuTien() < CustomerTier.REGULAR.getDoUuTien());
        }

        @Test
        @DisplayName("SaleStatus: có đúng 3 giá trị và valueOf đúng")
        void saleStatus() {
            assertEquals(3, SaleStatus.values().length);
            assertEquals(SaleStatus.SAP_DIEN_RA, SaleStatus.valueOf("SAP_DIEN_RA"));
            assertEquals(SaleStatus.DANG_DIEN_RA, SaleStatus.valueOf("DANG_DIEN_RA"));
            assertEquals(SaleStatus.DA_KET_THUC, SaleStatus.valueOf("DA_KET_THUC"));
        }

        @Test
        @DisplayName("OrderStatus: có đúng 4 giá trị và getMoTa() không null")
        void orderStatus() {
            assertEquals(4, OrderStatus.values().length);
            for (OrderStatus status : OrderStatus.values()) {
                assertNotNull(status.getMoTa(), status.name() + ".getMoTa() không được null");
            }
        }

        @Test
        @DisplayName("LockMechanism: có đúng 4 giá trị và valueOf đúng")
        void lockMechanism() {
            assertEquals(4, LockMechanism.values().length);
            assertEquals(LockMechanism.NO_LOCK, LockMechanism.valueOf("NO_LOCK"));
            assertEquals(LockMechanism.FILE_LOCK, LockMechanism.valueOf("FILE_LOCK"));
            assertEquals(LockMechanism.SYNCHRONIZED, LockMechanism.valueOf("SYNCHRONIZED"));
            assertEquals(LockMechanism.OPTIMISTIC, LockMechanism.valueOf("OPTIMISTIC"));
        }

        @Test
        @DisplayName("PaymentMethod: có đúng 3 giá trị và getMoTa() không null")
        void paymentMethod() {
            assertEquals(3, PaymentMethod.values().length);
            for (PaymentMethod pm : PaymentMethod.values()) {
                assertNotNull(pm.getMoTa(), pm.name() + ".getMoTa() không được null");
            }
        }
    }

    // ======================== BASE ENTITY ========================

    @Nested
    @DisplayName("📦 BaseEntity (equals, hashCode)")
    class BaseEntityTest {

        @Test
        @DisplayName("equals: cùng ID → true")
        void equalsSameId() {
            Product p1 = new Product("PRD-001", "A", ProductCategory.DIEN_TU, 100000, 10, 1);
            Product p2 = new Product("PRD-001", "B", ProductCategory.THOI_TRANG, 200000, 20, 2);
            assertEquals(p1, p2);
        }

        @Test
        @DisplayName("equals: khác ID → false")
        void equalsDifferentId() {
            Product p1 = new Product("PRD-001", "A", ProductCategory.DIEN_TU, 100000, 10, 1);
            Product p3 = new Product("PRD-002", "A", ProductCategory.DIEN_TU, 100000, 10, 1);
            assertNotEquals(p1, p3);
        }

        @Test
        @DisplayName("equals: so sánh với null → false")
        void equalsNull() {
            Product p1 = new Product("PRD-001", "A", ProductCategory.DIEN_TU, 100000, 10, 1);
            assertNotEquals(null, p1);
        }

        @Test
        @DisplayName("equals: khác class (Product vs Customer) → false")
        void equalsDifferentClass() {
            Product p1 = new Product("PRD-001", "A", ProductCategory.DIEN_TU, 100000, 10, 1);
            Customer c1 = new Customer("PRD-001", "Test", "t@t.com", CustomerTier.VIP, "2025-01-01");
            assertNotEquals(p1, c1);
        }

        @Test
        @DisplayName("equals: so sánh với chính nó → true")
        void equalsSelf() {
            Product p1 = new Product("PRD-001", "A", ProductCategory.DIEN_TU, 100000, 10, 1);
            assertEquals(p1, p1);
        }

        @Test
        @DisplayName("hashCode: cùng ID → cùng hashCode")
        void hashCodeSameId() {
            Product p1 = new Product("PRD-001", "A", ProductCategory.DIEN_TU, 100000, 10, 1);
            Product p2 = new Product("PRD-001", "B", ProductCategory.THOI_TRANG, 200000, 20, 2);
            assertEquals(p1.hashCode(), p2.hashCode());
        }
    }
}
