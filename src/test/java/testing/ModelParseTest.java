package testing;

import model.*;
import model.enums.*;

/**
 * Unit Test cho tất cả Entity — kiểm tra parse/serialize CSV round-trip.
 * 
 * Yêu cầu T3: "Unit test parse CSV đúng" — Test parse/serialize pass 100%.
 *
 * Mỗi entity được kiểm tra:
 * 1. toCsvLine() → fromCsvLine() → toCsvLine() phải cho kết quả giống nhau
 * 2. Mỗi trường sau parse phải giữ nguyên giá trị gốc
 * 3. getCsvHeader() phải trả về đúng format
 * 4. Các phương thức nghiệp vụ (coBanDuoc, soLuongConLai, thanhTien, ...) phải
 * đúng
 * 5. Edge case: giá trị biên, chuỗi rỗng, enum hợp lệ
 */
public class ModelParseTest {

        private static int tongTest = 0;
        private static int tongPass = 0;
        private static int tongFail = 0;

        // ======================== MAIN ========================

        public static void main(String[] args) {
                try {
                        System.setOut(new java.io.PrintStream(System.out, true, "UTF-8"));
                } catch (Exception e) {
                }
                System.out.println("╔══════════════════════════════════════════════════════════╗");
                System.out.println("║   🧪 UNIT TEST — MODEL LAYER PARSE/SERIALIZE           ║");
                System.out.println("║   Tuần 3: Kiểm tra tất cả Entity + Enum + BaseEntity   ║");
                System.out.println("╚══════════════════════════════════════════════════════════╝");
                System.out.println();

                long batDau = System.currentTimeMillis();

                // Test từng Entity
                testProduct();
                testCustomer();
                testFlashSaleEvent();
                testFlashSaleItem();
                testOrder();
                testOrderDetail();
                testOrderTransaction();

                // Test Enum
                testEnums();

                // Test BaseEntity (equals, hashCode)
                testBaseEntity();

                long ketThuc = System.currentTimeMillis();

                // ======= TỔNG KẾT =======
                System.out.println();
                System.out.println("═══════════════════════ TỔNG KẾT ═══════════════════════");
                System.out.printf("  Tổng test : %d%n", tongTest);
                System.out.printf("  ✅ Pass   : %d%n", tongPass);
                System.out.printf("  ❌ Fail   : %d%n", tongFail);
                System.out.printf("  Tỉ lệ    : %.1f%%%n", tongTest > 0 ? (tongPass * 100.0 / tongTest) : 0);
                System.out.printf("  Thời gian : %d ms%n", (ketThuc - batDau));
                System.out.println("═════════════════════════════════════════════════════════");

                if (tongFail > 0) {
                        System.out.println("\n  ❌ CÓ TEST THẤT BẠI — Cần kiểm tra lại!");
                        System.exit(1);
                } else {
                        System.out.println("\n  ✅ TẤT CẢ TEST ĐỀU PASS!");
                }
        }

        // ======================== TEST PRODUCT ========================

        private static void testProduct() {
                printHeader("PRODUCT");

                // Test 1: Round-trip cơ bản
                Product p = new Product("PRD-00001", "Tai nghe Bluetooth DT-0001",
                                ProductCategory.DIEN_TU, 500000.0, 100, 1);

                String csv = p.toCsvLine();
                Product p2 = new Product();
                p2.fromCsvLine(csv);

                assertEqual("Product round-trip: toCsvLine match",
                                csv, p2.toCsvLine());
                assertEqual("Product: productId",
                                "PRD-00001", p2.getProductId());
                assertEqual("Product: name",
                                "Tai nghe Bluetooth DT-0001", p2.getName());
                assertEqual("Product: category",
                                ProductCategory.DIEN_TU, p2.getCategory());
                assertDoubleEqual("Product: originalPrice",
                                500000.0, p2.getOriginalPrice());
                assertIntEqual("Product: stock",
                                100, p2.getStock());
                assertIntEqual("Product: version",
                                1, p2.getVersion());

                // Test 2: ID mapping
                assertEqual("Product: id == productId",
                                p2.getProductId(), p2.getId());

                // Test 3: CSV Header
                assertEqual("Product: getCsvHeader()",
                                "productId,name,category,originalPrice,stock,version",
                                p2.getCsvHeader());

                // Test 4: Giá trị lớn
                Product p3 = new Product("PRD-99999", "Máy tính bảng DT-9999",
                                ProductCategory.DIEN_TU, 4999000.0, 999, 50);
                String csv3 = p3.toCsvLine();
                Product p4 = new Product();
                p4.fromCsvLine(csv3);
                assertEqual("Product round-trip: giá trị lớn",
                                csv3, p4.toCsvLine());

                // Test 5: Mỗi danh mục
                for (ProductCategory cat : ProductCategory.values()) {
                        Product pc = new Product("P-" + cat.name(), "Test " + cat.name(),
                                        cat, 100000.0, 10, 1);
                        Product pc2 = new Product();
                        pc2.fromCsvLine(pc.toCsvLine());
                        assertEqual("Product category round-trip: " + cat.name(),
                                        cat, pc2.getCategory());
                }

                // Test 6: toString() không throw
                assertNoException("Product: toString()", () -> p.toString());

                // Test 7: Setter test
                Product ps = new Product();
                ps.setProductId("PRD-SET");
                assertEqual("Product: setProductId → getId()",
                                "PRD-SET", ps.getId());

                System.out.println();
        }

        // ======================== TEST CUSTOMER ========================

        private static void testCustomer() {
                printHeader("CUSTOMER");

                // Test 1: Round-trip
                Customer c = new Customer("CUS-00001", "Nguyễn Văn An",
                                "an.nguyen1@email.com", CustomerTier.VIP, "2024-05-15");

                String csv = c.toCsvLine();
                Customer c2 = new Customer();
                c2.fromCsvLine(csv);

                assertEqual("Customer round-trip: toCsvLine match",
                                csv, c2.toCsvLine());
                assertEqual("Customer: customerId",
                                "CUS-00001", c2.getCustomerId());
                assertEqual("Customer: name",
                                "Nguyễn Văn An", c2.getName());
                assertEqual("Customer: email",
                                "an.nguyen1@email.com", c2.getEmail());
                assertEqual("Customer: tier",
                                CustomerTier.VIP, c2.getTier());
                assertEqual("Customer: registeredDate",
                                "2024-05-15", c2.getRegisteredDate());

                // Test 2: ID mapping
                assertEqual("Customer: id == customerId",
                                c2.getCustomerId(), c2.getId());

                // Test 3: Header
                assertEqual("Customer: getCsvHeader()",
                                "customerId,name,email,tier,registeredDate",
                                c2.getCsvHeader());

                // Test 4: Mỗi hạng
                for (CustomerTier tier : CustomerTier.values()) {
                        Customer ct = new Customer("C-" + tier.name(), "Test", "t@t.com",
                                        tier, "2025-01-01");
                        Customer ct2 = new Customer();
                        ct2.fromCsvLine(ct.toCsvLine());
                        assertEqual("Customer tier round-trip: " + tier.name(),
                                        tier, ct2.getTier());
                }

                // Test 5: Tên có dấu tiếng Việt
                Customer cVn = new Customer("CUS-00002", "Trần Thị Phương Thảo",
                                "thao.tran@email.com", CustomerTier.PREMIUM, "2025-03-20");
                Customer cVn2 = new Customer();
                cVn2.fromCsvLine(cVn.toCsvLine());
                assertEqual("Customer: tên tiếng Việt round-trip",
                                "Trần Thị Phương Thảo", cVn2.getName());

                // Test 6: toString() không throw
                assertNoException("Customer: toString()", () -> c.toString());

                System.out.println();
        }

        // ======================== TEST FLASH SALE EVENT ========================

        private static void testFlashSaleEvent() {
                printHeader("FLASH SALE EVENT");

                // Test 1: Round-trip
                FlashSaleEvent e = new FlashSaleEvent("EVT-001", "Flash Sale Mùa Hè #1",
                                "2026-06-15T10:00:00", "2026-06-15T12:00:00",
                                SaleStatus.DANG_DIEN_RA, 50);

                String csv = e.toCsvLine();
                FlashSaleEvent e2 = new FlashSaleEvent();
                e2.fromCsvLine(csv);

                assertEqual("FlashSaleEvent round-trip: toCsvLine match",
                                csv, e2.toCsvLine());
                assertEqual("FlashSaleEvent: eventId",
                                "EVT-001", e2.getEventId());
                assertEqual("FlashSaleEvent: eventName",
                                "Flash Sale Mùa Hè #1", e2.getEventName());
                assertEqual("FlashSaleEvent: startTime",
                                "2026-06-15T10:00:00", e2.getStartTime());
                assertEqual("FlashSaleEvent: endTime",
                                "2026-06-15T12:00:00", e2.getEndTime());
                assertEqual("FlashSaleEvent: status",
                                SaleStatus.DANG_DIEN_RA, e2.getStatus());
                assertIntEqual("FlashSaleEvent: discountPercent",
                                50, e2.getDiscountPercent());

                // Test 2: ID mapping
                assertEqual("FlashSaleEvent: id == eventId",
                                e2.getEventId(), e2.getId());

                // Test 3: Header
                assertEqual("FlashSaleEvent: getCsvHeader()",
                                "eventId,eventName,startTime,endTime,status,discountPercent",
                                e2.getCsvHeader());

                // Test 4: Mỗi trạng thái
                for (SaleStatus status : SaleStatus.values()) {
                        FlashSaleEvent es = new FlashSaleEvent("E-" + status.name(), "Test",
                                        "2026-01-01T00:00:00", "2026-01-01T02:00:00", status, 30);
                        FlashSaleEvent es2 = new FlashSaleEvent();
                        es2.fromCsvLine(es.toCsvLine());
                        assertEqual("FlashSaleEvent status round-trip: " + status.name(),
                                        status, es2.getStatus());
                }

                // Test 5: Biên giảm giá (30% và 70%)
                FlashSaleEvent eLow = new FlashSaleEvent("EVT-L", "Sale30", "2026-01-01T00:00:00",
                                "2026-01-01T02:00:00", SaleStatus.SAP_DIEN_RA, 30);
                FlashSaleEvent eLow2 = new FlashSaleEvent();
                eLow2.fromCsvLine(eLow.toCsvLine());
                assertIntEqual("FlashSaleEvent: discount 30%", 30, eLow2.getDiscountPercent());

                FlashSaleEvent eHigh = new FlashSaleEvent("EVT-H", "Sale70", "2026-01-01T00:00:00",
                                "2026-01-01T02:00:00", SaleStatus.DA_KET_THUC, 70);
                FlashSaleEvent eHigh2 = new FlashSaleEvent();
                eHigh2.fromCsvLine(eHigh.toCsvLine());
                assertIntEqual("FlashSaleEvent: discount 70%", 70, eHigh2.getDiscountPercent());

                // Test 6: toString() không throw
                assertNoException("FlashSaleEvent: toString()", () -> e.toString());

                System.out.println();
        }

        // ======================== TEST FLASH SALE ITEM ========================

        private static void testFlashSaleItem() {
                printHeader("FLASH SALE ITEM (CRITICAL)");

                // Test 1: Round-trip
                FlashSaleItem fi = new FlashSaleItem("FSI-00001", "EVT-001", "PRD-00001",
                                50, 10, 250000.0, 3);

                String csv = fi.toCsvLine();
                FlashSaleItem fi2 = new FlashSaleItem();
                fi2.fromCsvLine(csv);

                assertEqual("FlashSaleItem round-trip: toCsvLine match",
                                csv, fi2.toCsvLine());
                assertEqual("FlashSaleItem: flashItemId",
                                "FSI-00001", fi2.getFlashItemId());
                assertEqual("FlashSaleItem: eventId",
                                "EVT-001", fi2.getEventId());
                assertEqual("FlashSaleItem: productId",
                                "PRD-00001", fi2.getProductId());
                assertIntEqual("FlashSaleItem: limitedQty",
                                50, fi2.getLimitedQty());
                assertIntEqual("FlashSaleItem: soldQty",
                                10, fi2.getSoldQty());
                assertDoubleEqual("FlashSaleItem: flashPrice",
                                250000.0, fi2.getFlashPrice());
                assertIntEqual("FlashSaleItem: version",
                                3, fi2.getVersion());

                // Test 2: ID mapping
                assertEqual("FlashSaleItem: id == flashItemId",
                                fi2.getFlashItemId(), fi2.getId());

                // Test 3: Header
                assertEqual("FlashSaleItem: getCsvHeader()",
                                "flashItemId,eventId,productId,limitedQty,soldQty,flashPrice,version",
                                fi2.getCsvHeader());

                // Test 4: coBanDuoc() — kiểm tra nghiệp vụ
                FlashSaleItem fiTest = new FlashSaleItem("FSI-TEST", "EVT-001", "PRD-001",
                                10, 8, 100000.0, 1);

                assertTrue("FlashSaleItem: coBanDuoc(2) khi soldQty=8, limit=10",
                                fiTest.coBanDuoc(2)); // 8+2 = 10 <= 10 → true
                assertFalse("FlashSaleItem: coBanDuoc(3) khi soldQty=8, limit=10",
                                fiTest.coBanDuoc(3)); // 8+3 = 11 > 10 → false
                assertTrue("FlashSaleItem: coBanDuoc(1) khi soldQty=8, limit=10",
                                fiTest.coBanDuoc(1)); // 8+1 = 9 <= 10 → true

                // Test 5: soLuongConLai()
                assertIntEqual("FlashSaleItem: soLuongConLai() = limit - sold",
                                2, fiTest.soLuongConLai()); // 10 - 8 = 2

                // Test 6: Edge case — đã bán hết
                FlashSaleItem fiFull = new FlashSaleItem("FSI-FULL", "EVT-001", "PRD-001",
                                5, 5, 100000.0, 1);
                assertFalse("FlashSaleItem: coBanDuoc(1) khi đã bán hết",
                                fiFull.coBanDuoc(1));
                assertIntEqual("FlashSaleItem: soLuongConLai() = 0 khi đã bán hết",
                                0, fiFull.soLuongConLai());

                // Test 7: Edge case — chưa bán gì
                FlashSaleItem fiEmpty = new FlashSaleItem("FSI-EMPTY", "EVT-001", "PRD-001",
                                100, 0, 500000.0, 1);
                assertTrue("FlashSaleItem: coBanDuoc(100) khi chưa bán gì",
                                fiEmpty.coBanDuoc(100));
                assertIntEqual("FlashSaleItem: soLuongConLai() = 100 khi chưa bán",
                                100, fiEmpty.soLuongConLai());

                // Test 8: toString() không throw
                assertNoException("FlashSaleItem: toString()", () -> fi.toString());

                System.out.println();
        }

        // ======================== TEST ORDER ========================

        private static void testOrder() {
                printHeader("ORDER");

                // Test 1: Round-trip
                Order o = new Order("ORD-00001", "CUS-00001", "EVT-001",
                                "2026-06-15T10:05:30", OrderStatus.DA_XAC_NHAN, 750000.0);

                String csv = o.toCsvLine();
                Order o2 = new Order();
                o2.fromCsvLine(csv);

                assertEqual("Order round-trip: toCsvLine match",
                                csv, o2.toCsvLine());
                assertEqual("Order: orderId",
                                "ORD-00001", o2.getOrderId());
                assertEqual("Order: customerId",
                                "CUS-00001", o2.getCustomerId());
                assertEqual("Order: eventId",
                                "EVT-001", o2.getEventId());
                assertEqual("Order: orderTime",
                                "2026-06-15T10:05:30", o2.getOrderTime());
                assertEqual("Order: status",
                                OrderStatus.DA_XAC_NHAN, o2.getStatus());
                assertDoubleEqual("Order: totalAmount",
                                750000.0, o2.getTotalAmount());

                // Test 2: ID mapping
                assertEqual("Order: id == orderId",
                                o2.getOrderId(), o2.getId());

                // Test 3: Header
                assertEqual("Order: getCsvHeader()",
                                "orderId,customerId,eventId,orderTime,status,totalAmount",
                                o2.getCsvHeader());

                // Test 4: Mỗi trạng thái
                for (OrderStatus status : OrderStatus.values()) {
                        Order os = new Order("O-" + status.name(), "CUS-001", "EVT-001",
                                        "2026-01-01T00:00:00", status, 100000.0);
                        Order os2 = new Order();
                        os2.fromCsvLine(os.toCsvLine());
                        assertEqual("Order status round-trip: " + status.name(),
                                        status, os2.getStatus());
                }

                // Test 5: toString() không throw
                assertNoException("Order: toString()", () -> o.toString());

                System.out.println();
        }

        // ======================== TEST ORDER DETAIL ========================

        private static void testOrderDetail() {
                printHeader("ORDER DETAIL");

                // Test 1: Round-trip
                OrderDetail d = new OrderDetail("DTL-00001", "ORD-00001", "FSI-00001",
                                2, 250000.0);

                String csv = d.toCsvLine();
                OrderDetail d2 = new OrderDetail();
                d2.fromCsvLine(csv);

                assertEqual("OrderDetail round-trip: toCsvLine match",
                                csv, d2.toCsvLine());
                assertEqual("OrderDetail: detailId",
                                "DTL-00001", d2.getDetailId());
                assertEqual("OrderDetail: orderId",
                                "ORD-00001", d2.getOrderId());
                assertEqual("OrderDetail: flashItemId",
                                "FSI-00001", d2.getFlashItemId());
                assertIntEqual("OrderDetail: quantity",
                                2, d2.getQuantity());
                assertDoubleEqual("OrderDetail: unitPrice",
                                250000.0, d2.getUnitPrice());

                // Test 2: ID mapping
                assertEqual("OrderDetail: id == detailId",
                                d2.getDetailId(), d2.getId());

                // Test 3: Header
                assertEqual("OrderDetail: getCsvHeader()",
                                "detailId,orderId,flashItemId,quantity,unitPrice",
                                d2.getCsvHeader());

                // Test 4: thanhTien()
                assertDoubleEqual("OrderDetail: thanhTien() = quantity × unitPrice",
                                500000.0, d2.thanhTien()); // 2 × 250000 = 500000

                // Test 5: thanhTien() với quantity=1
                OrderDetail d3 = new OrderDetail("DTL-00002", "ORD-00002", "FSI-00002",
                                1, 1500000.0);
                assertDoubleEqual("OrderDetail: thanhTien() quantity=1",
                                1500000.0, d3.thanhTien());

                // Test 6: toString() không throw
                assertNoException("OrderDetail: toString()", () -> d.toString());

                System.out.println();
        }

        // ======================== TEST ORDER TRANSACTION ========================

        private static void testOrderTransaction() {
                printHeader("ORDER TRANSACTION");

                // Test 1: Round-trip — thành công
                OrderTransaction t = new OrderTransaction("TXN-00001", "ORD-00001",
                                LockMechanism.OPTIMISTIC, "Thread-1",
                                1000000000L, 1005000000L, true, "");

                String csv = t.toCsvLine();
                OrderTransaction t2 = new OrderTransaction();
                t2.fromCsvLine(csv);

                assertEqual("OrderTransaction round-trip: toCsvLine match",
                                csv, t2.toCsvLine());
                assertEqual("OrderTransaction: transactionId",
                                "TXN-00001", t2.getTransactionId());
                assertEqual("OrderTransaction: orderId",
                                "ORD-00001", t2.getOrderId());
                assertEqual("OrderTransaction: mechanism",
                                LockMechanism.OPTIMISTIC, t2.getMechanism());
                assertEqual("OrderTransaction: threadName",
                                "Thread-1", t2.getThreadName());
                assertLongEqual("OrderTransaction: startTime",
                                1000000000L, t2.getStartTime());
                assertLongEqual("OrderTransaction: endTime",
                                1005000000L, t2.getEndTime());
                assertTrue("OrderTransaction: success == true",
                                t2.isSuccess());

                // Test 2: Round-trip — thất bại với error message
                OrderTransaction tFail = new OrderTransaction("TXN-00002", "ORD-00002",
                                LockMechanism.NO_LOCK, "Thread-5",
                                2000000000L, 2001000000L, false, "Hết hàng");

                String csvFail = tFail.toCsvLine();
                OrderTransaction tFail2 = new OrderTransaction();
                tFail2.fromCsvLine(csvFail);

                assertEqual("OrderTransaction fail round-trip: toCsvLine match",
                                csvFail, tFail2.toCsvLine());
                assertFalse("OrderTransaction: success == false",
                                tFail2.isSuccess());
                assertEqual("OrderTransaction: errorMessage",
                                "Hết hàng", tFail2.getErrorMessage());

                // Test 3: ID mapping
                assertEqual("OrderTransaction: id == transactionId",
                                t2.getTransactionId(), t2.getId());

                // Test 4: Header
                assertEqual("OrderTransaction: getCsvHeader()",
                                "transactionId,orderId,mechanism,threadName,startTime,endTime,success,errorMessage",
                                t2.getCsvHeader());

                // Test 5: thoiGianXuLyMs()
                // (1005000000 - 1000000000) / 1_000_000.0 = 5.0 ms
                assertDoubleEqual("OrderTransaction: thoiGianXuLyMs()",
                                5.0, t.thoiGianXuLyMs());

                // Test 6: Mỗi cơ chế
                for (LockMechanism mech : LockMechanism.values()) {
                        OrderTransaction tm = new OrderTransaction("T-" + mech.name(), "ORD-001",
                                        mech, "Thread-X", 0L, 1000000L, true, "");
                        OrderTransaction tm2 = new OrderTransaction();
                        tm2.fromCsvLine(tm.toCsvLine());
                        assertEqual("OrderTransaction mechanism round-trip: " + mech.name(),
                                        mech, tm2.getMechanism());
                }

                // Test 7: toString() không throw
                assertNoException("OrderTransaction: toString()", () -> t.toString());

                System.out.println();
        }

        // ======================== TEST ENUMS ========================

        private static void testEnums() {
                printHeader("ENUMS");

                // Test ProductCategory
                assertIntEqual("ProductCategory: values count", 6, ProductCategory.values().length);
                for (ProductCategory cat : ProductCategory.values()) {
                        assertNotNull("ProductCategory: " + cat.name() + ".getMoTa()", cat.getMoTa());
                }

                // Test CustomerTier
                assertIntEqual("CustomerTier: values count", 3, CustomerTier.values().length);
                assertTrue("CustomerTier: VIP ưu tiên > PREMIUM",
                                CustomerTier.VIP.getDoUuTien() < CustomerTier.PREMIUM.getDoUuTien());
                assertTrue("CustomerTier: PREMIUM ưu tiên > REGULAR",
                                CustomerTier.PREMIUM.getDoUuTien() < CustomerTier.REGULAR.getDoUuTien());

                // Test SaleStatus
                assertIntEqual("SaleStatus: values count", 3, SaleStatus.values().length);
                assertEqual("SaleStatus: valueOf SAP_DIEN_RA",
                                SaleStatus.SAP_DIEN_RA, SaleStatus.valueOf("SAP_DIEN_RA"));
                assertEqual("SaleStatus: valueOf DANG_DIEN_RA",
                                SaleStatus.DANG_DIEN_RA, SaleStatus.valueOf("DANG_DIEN_RA"));
                assertEqual("SaleStatus: valueOf DA_KET_THUC",
                                SaleStatus.DA_KET_THUC, SaleStatus.valueOf("DA_KET_THUC"));

                // Test OrderStatus
                assertIntEqual("OrderStatus: values count", 4, OrderStatus.values().length);
                for (OrderStatus status : OrderStatus.values()) {
                        assertNotNull("OrderStatus: " + status.name() + ".getMoTa()", status.getMoTa());
                }

                // Test LockMechanism
                assertIntEqual("LockMechanism: values count", 4, LockMechanism.values().length);
                assertEqual("LockMechanism: valueOf NO_LOCK",
                                LockMechanism.NO_LOCK, LockMechanism.valueOf("NO_LOCK"));
                assertEqual("LockMechanism: valueOf FILE_LOCK",
                                LockMechanism.FILE_LOCK, LockMechanism.valueOf("FILE_LOCK"));
                assertEqual("LockMechanism: valueOf SYNCHRONIZED",
                                LockMechanism.SYNCHRONIZED, LockMechanism.valueOf("SYNCHRONIZED"));
                assertEqual("LockMechanism: valueOf OPTIMISTIC",
                                LockMechanism.OPTIMISTIC, LockMechanism.valueOf("OPTIMISTIC"));

                // Test PaymentMethod
                assertIntEqual("PaymentMethod: values count", 3, PaymentMethod.values().length);
                for (PaymentMethod pm : PaymentMethod.values()) {
                        assertNotNull("PaymentMethod: " + pm.name() + ".getMoTa()", pm.getMoTa());
                }

                System.out.println();
        }

        // ======================== TEST BASE ENTITY ========================

        private static void testBaseEntity() {
                printHeader("BASE ENTITY (equals, hashCode)");

                // Test equals: cùng ID
                Product p1 = new Product("PRD-001", "A", ProductCategory.DIEN_TU, 100000, 10, 1);
                Product p2 = new Product("PRD-001", "B", ProductCategory.THOI_TRANG, 200000, 20, 2);
                assertTrue("BaseEntity: equals cùng ID", p1.equals(p2));

                // Test equals: khác ID
                Product p3 = new Product("PRD-002", "A", ProductCategory.DIEN_TU, 100000, 10, 1);
                assertFalse("BaseEntity: equals khác ID", p1.equals(p3));

                // Test equals: null
                assertFalse("BaseEntity: equals null", p1.equals(null));

                // Test equals: khác class
                Customer c1 = new Customer("PRD-001", "Test", "t@t.com", CustomerTier.VIP, "2025-01-01");
                assertFalse("BaseEntity: equals khác class (Product vs Customer)", p1.equals(c1));

                // Test equals: chính nó
                assertTrue("BaseEntity: equals chính nó", p1.equals(p1));

                // Test hashCode: cùng ID → cùng hashCode
                assertIntEqual("BaseEntity: hashCode cùng ID",
                                p1.hashCode(), p2.hashCode());

                System.out.println();
        }

        // ======================== ASSERTION HELPERS ========================

        private static void assertEqual(String testName, Object expected, Object actual) {
                tongTest++;
                if (expected == null && actual == null) {
                        tongPass++;
                        printPass(testName);
                } else if (expected != null && expected.equals(actual)) {
                        tongPass++;
                        printPass(testName);
                } else {
                        tongFail++;
                        printFail(testName, String.valueOf(expected), String.valueOf(actual));
                }
        }

        private static void assertIntEqual(String testName, int expected, int actual) {
                tongTest++;
                if (expected == actual) {
                        tongPass++;
                        printPass(testName);
                } else {
                        tongFail++;
                        printFail(testName, String.valueOf(expected), String.valueOf(actual));
                }
        }

        private static void assertLongEqual(String testName, long expected, long actual) {
                tongTest++;
                if (expected == actual) {
                        tongPass++;
                        printPass(testName);
                } else {
                        tongFail++;
                        printFail(testName, String.valueOf(expected), String.valueOf(actual));
                }
        }

        private static void assertDoubleEqual(String testName, double expected, double actual) {
                tongTest++;
                if (Math.abs(expected - actual) < 0.01) {
                        tongPass++;
                        printPass(testName);
                } else {
                        tongFail++;
                        printFail(testName, String.valueOf(expected), String.valueOf(actual));
                }
        }

        private static void assertTrue(String testName, boolean condition) {
                tongTest++;
                if (condition) {
                        tongPass++;
                        printPass(testName);
                } else {
                        tongFail++;
                        printFail(testName, "true", "false");
                }
        }

        private static void assertFalse(String testName, boolean condition) {
                tongTest++;
                if (!condition) {
                        tongPass++;
                        printPass(testName);
                } else {
                        tongFail++;
                        printFail(testName, "false", "true");
                }
        }

        private static void assertNotNull(String testName, Object obj) {
                tongTest++;
                if (obj != null) {
                        tongPass++;
                        printPass(testName);
                } else {
                        tongFail++;
                        printFail(testName, "not null", "null");
                }
        }

        private static void assertNoException(String testName, Runnable action) {
                tongTest++;
                try {
                        action.run();
                        tongPass++;
                        printPass(testName);
                } catch (Exception e) {
                        tongFail++;
                        printFail(testName, "no exception", e.getClass().getSimpleName() + ": " + e.getMessage());
                }
        }

        // ======================== PRINT HELPERS ========================

        private static void printHeader(String section) {
                System.out.println("  ┌─────────────────────────────────────────────┐");
                System.out.printf("  │  📦 %-40s │%n", section);
                System.out.println("  └─────────────────────────────────────────────┘");
        }

        private static void printPass(String testName) {
                System.out.printf("    ✅ %s%n", testName);
        }

        private static void printFail(String testName, String expected, String actual) {
                System.out.printf("    ❌ %s%n", testName);
                System.out.printf("       Expected: %s%n", expected);
                System.out.printf("       Actual  : %s%n", actual);
        }
}
