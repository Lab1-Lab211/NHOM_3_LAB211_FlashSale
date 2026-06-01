package util;

import model.*;
import model.enums.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Công cụ sinh dữ liệu CSV mẫu cho hệ thống Flash Sale.
 * Tổng dữ liệu sinh ra >= 10,000 dòng theo yêu cầu đề bài.
 *
 * Phân bổ:
 *   - products.csv      : 5,000 dòng
 *   - customers.csv     : 2,000 dòng
 *   - flash_events.csv  : 20 dòng
 *   - flash_items.csv   : 500 dòng
 *   - orders.csv        : 2,500 dòng
 *   - order_details.csv : 2,500 dòng
 *   => TỔNG: 12,520 dòng (>= 10,000)
 */
public class DataGenerator {

    private static final Random random = new Random(42); // seed cố định để tái tạo
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // === DỮ LIỆU MẪU TIẾNG VIỆT ===

    private static final String[] HO = {
        "Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Huỳnh", "Phan", "Vũ",
        "Võ", "Đặng", "Bùi", "Đỗ", "Hồ", "Ngô", "Dương", "Lý"
    };
    private static final String[] TEN_DEM = {
        "Văn", "Thị", "Hoàng", "Minh", "Thanh", "Đức", "Quốc", "Ngọc",
        "Phương", "Hữu", "Xuân", "Thu", "Bảo", "Gia", "Anh"
    };
    private static final String[] TEN = {
        "An", "Bình", "Chi", "Dũng", "Hà", "Hùng", "Lan", "Minh",
        "Nam", "Phúc", "Quân", "Tâm", "Thảo", "Tú", "Vy", "Yến",
        "Hải", "Long", "Sơn", "Trung", "Tuấn", "Linh", "Mai", "Đạt"
    };

    // Tên sản phẩm theo danh mục
    private static final Map<ProductCategory, String[]> TEN_SAN_PHAM = new LinkedHashMap<>();
    static {
        TEN_SAN_PHAM.put(ProductCategory.DIEN_TU, new String[]{
            "Tai nghe Bluetooth", "Chuột gaming", "Bàn phím cơ", "Màn hình LED",
            "Ổ cứng SSD", "USB 64GB", "Loa di động", "Sạc dự phòng",
            "Camera hành trình", "Đồng hồ thông minh", "Máy tính bảng",
            "Ốp lưng điện thoại", "Cáp sạc nhanh", "Hub USB-C", "Webcam HD",
            "Micro thu âm", "Đèn LED bàn", "Quạt mini USB", "Pin sạc AA",
            "Adapter WiFi", "Ổ cắm thông minh", "Chuông cửa thông minh"
        });
        TEN_SAN_PHAM.put(ProductCategory.THOI_TRANG, new String[]{
            "Áo thun nam", "Quần jean nữ", "Giày sneaker", "Túi xách da",
            "Đồng hồ thời trang", "Kính mát", "Mũ lưỡi trai", "Thắt lưng da",
            "Áo khoác gió", "Váy liền", "Áo sơ mi", "Quần short",
            "Giày cao gót", "Balo thời trang", "Vớ cotton", "Khăn quàng cổ",
            "Áo hoodie", "Quần jogger", "Sandal đế bằng", "Nón bucket"
        });
        TEN_SAN_PHAM.put(ProductCategory.GIA_DUNG, new String[]{
            "Nồi cơm điện", "Máy xay sinh tố", "Bộ chăn ga", "Đèn LED trang trí",
            "Bình giữ nhiệt", "Chảo chống dính", "Máy hút bụi mini", "Quạt điện",
            "Bộ dao nhà bếp", "Thớt gỗ", "Ấm siêu tốc", "Máy sấy tóc",
            "Giá treo quần áo", "Hộp đựng thực phẩm", "Rèm cửa", "Gối ngủ"
        });
        TEN_SAN_PHAM.put(ProductCategory.LAM_DEP, new String[]{
            "Kem chống nắng", "Sữa rửa mặt", "Son môi", "Nước hoa",
            "Mặt nạ dưỡng da", "Serum vitamin C", "Kem dưỡng ẩm", "Tẩy trang",
            "Phấn phủ", "Mascara", "Bộ cọ trang điểm", "Sữa tắm",
            "Dầu gội thảo dược", "Tinh dầu dưỡng tóc", "Kem nền", "Xịt khoáng"
        });
        TEN_SAN_PHAM.put(ProductCategory.THUC_PHAM, new String[]{
            "Hạt điều rang muối", "Trà xanh hữu cơ", "Cà phê Arabica",
            "Mật ong nguyên chất", "Granola ngũ cốc", "Bánh tráng cuốn",
            "Nước mắm truyền thống", "Tương ớt", "Trái cây sấy",
            "Socola đen", "Sữa hạt", "Bột matcha", "Kẹo dừa Bến Tre",
            "Mì ăn liền", "Bánh pía", "Khô bò miếng"
        });
        TEN_SAN_PHAM.put(ProductCategory.THE_THAO, new String[]{
            "Giày chạy bộ", "Bóng đá", "Vợt cầu lông", "Thảm yoga",
            "Tạ tay 5kg", "Dây nhảy", "Bình nước thể thao", "Găng tay gym",
            "Áo thể thao", "Quần thể thao", "Bóng rổ", "Kính bơi",
            "Mũ bơi", "Ba lô leo núi", "Đai bảo vệ đầu gối", "Bóng bàn"
        });
    }

    private static final String[] TEN_SU_KIEN = {
        "Flash Sale Mùa Hè", "Siêu Sale Cuối Tuần", "Deal Sốc Nửa Đêm",
        "Ngày Hội Mua Sắm", "Sale Chào Tháng Mới", "Flash Deal 12.12",
        "Giảm Giá Sinh Nhật", "Black Friday Việt Nam", "Sale Tết Dương Lịch",
        "Deal Hot Thứ 6", "Săn Sale Đầu Tháng", "Flash Sale Công Nghệ",
        "Giảm Giá Thời Trang", "Sale Gia Dụng", "Deal Làm Đẹp",
        "Flash Sale Thể Thao", "Siêu Ưu Đãi VIP", "Sale Cuối Mùa",
        "Ngày Vàng Khuyến Mãi", "Deal Chớp Nhoáng"
    };

    // === ĐƯỜNG DẪN THƯ MỤC DỮ LIỆU ===
    private static final String DATA_DIR = "data";

    // ======================== MAIN ========================

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║   ⚡ CÔNG CỤ SINH DỮ LIỆU FLASH SALE      ║");
        System.out.println("╠══════════════════════════════════════════════╣");
        System.out.println("║   Bắt đầu sinh dữ liệu CSV...             ║");
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.println();

        long batDau = System.currentTimeMillis();

        // Tạo thư mục data nếu chưa có
        new File(DATA_DIR).mkdirs();

        // Sinh dữ liệu theo thứ tự phụ thuộc (đảm bảo khóa ngoại hợp lệ)
        List<Product> products = sinhSanPham(5000);
        ghiFile(DATA_DIR + "/products.csv", products);

        List<Customer> customers = sinhKhachHang(2000);
        ghiFile(DATA_DIR + "/customers.csv", customers);

        List<FlashSaleEvent> events = sinhSuKien(20);
        ghiFile(DATA_DIR + "/flash_events.csv", events);

        List<FlashSaleItem> flashItems = sinhFlashItem(500, events, products);
        ghiFile(DATA_DIR + "/flash_items.csv", flashItems);

        List<Order> orders = sinhDonHang(2500, customers, events);
        ghiFile(DATA_DIR + "/orders.csv", orders);

        List<OrderDetail> orderDetails = sinhChiTietDonHang(2500, orders, flashItems);
        ghiFile(DATA_DIR + "/order_details.csv", orderDetails);

        long ketThuc = System.currentTimeMillis();

        // Tổng kết
        int tongDong = products.size() + customers.size() + events.size()
                + flashItems.size() + orders.size() + orderDetails.size();

        System.out.println();
        System.out.println("═══════════════ KẾT QUẢ ═══════════════");
        System.out.printf("  ✅ products.csv      : %,d dòng%n", products.size());
        System.out.printf("  ✅ customers.csv     : %,d dòng%n", customers.size());
        System.out.printf("  ✅ flash_events.csv  : %,d dòng%n", events.size());
        System.out.printf("  ✅ flash_items.csv   : %,d dòng%n", flashItems.size());
        System.out.printf("  ✅ orders.csv        : %,d dòng%n", orders.size());
        System.out.printf("  ✅ order_details.csv : %,d dòng%n", orderDetails.size());
        System.out.println("  ─────────────────────────────────────");
        System.out.printf("  📊 TỔNG CỘNG        : %,d dòng%n", tongDong);
        System.out.printf("  ⏱  Thời gian        : %,d ms%n", (ketThuc - batDau));
        System.out.println("════════════════════════════════════════");

        // Kiểm tra parse round-trip
        System.out.println();
        kiemTraRoundTrip(products, customers, events, flashItems, orders, orderDetails);
    }

    // ======================== SINH DỮ LIỆU ========================

    /** Sinh danh sách sản phẩm với tên tiếng Việt */
    private static List<Product> sinhSanPham(int soLuong) {
        System.out.printf("  🔄 Đang sinh %,d sản phẩm...%n", soLuong);
        List<Product> ds = new ArrayList<>();
        ProductCategory[] categories = ProductCategory.values();

        for (int i = 1; i <= soLuong; i++) {
            ProductCategory cat = categories[random.nextInt(categories.length)];
            String[] tenTheoLoai = TEN_SAN_PHAM.get(cat);
            String tenGoc = tenTheoLoai[random.nextInt(tenTheoLoai.length)];
            // Thêm biến thể để tên không trùng
            String ten = tenGoc + " " + maoChu(cat) + "-" + String.format("%04d", i);
            double giaGoc = lamTronGia(50000 + random.nextDouble() * 4950000); // 50k - 5tr VNĐ
            int tonKho = 10 + random.nextInt(991); // 10 - 1000

            ds.add(new Product(
                    String.format("PRD-%05d", i),
                    ten, cat, giaGoc, tonKho, 1
            ));
        }
        return ds;
    }

    /** Sinh danh sách khách hàng với tên tiếng Việt */
    private static List<Customer> sinhKhachHang(int soLuong) {
        System.out.printf("  🔄 Đang sinh %,d khách hàng...%n", soLuong);
        List<Customer> ds = new ArrayList<>();

        for (int i = 1; i <= soLuong; i++) {
            String ho = HO[random.nextInt(HO.length)];
            String tenDem = TEN_DEM[random.nextInt(TEN_DEM.length)];
            String ten = TEN[random.nextInt(TEN.length)];
            String hoTen = ho + " " + tenDem + " " + ten;

            // Email: loại bỏ dấu tiếng Việt
            String email = boDau(ten.toLowerCase()) + "." + boDau(ho.toLowerCase())
                    + i + "@email.com";

            // Hạng: 10% VIP, 20% PREMIUM, 70% REGULAR
            CustomerTier tier;
            int r = random.nextInt(100);
            if (r < 10) tier = CustomerTier.VIP;
            else if (r < 30) tier = CustomerTier.PREMIUM;
            else tier = CustomerTier.REGULAR;

            // Ngày đăng ký: trong 2 năm gần đây
            LocalDate ngayDK = LocalDate.of(2024, 1, 1).plusDays(random.nextInt(730));

            ds.add(new Customer(
                    String.format("CUS-%05d", i),
                    hoTen, email, tier, ngayDK.format(DATE_FMT)
            ));
        }
        return ds;
    }

    /** Sinh danh sách sự kiện Flash Sale */
    private static List<FlashSaleEvent> sinhSuKien(int soLuong) {
        System.out.printf("  🔄 Đang sinh %,d sự kiện Flash Sale...%n", soLuong);
        List<FlashSaleEvent> ds = new ArrayList<>();

        for (int i = 1; i <= soLuong; i++) {
            String ten = TEN_SU_KIEN[(i - 1) % TEN_SU_KIEN.length] + " #" + i;

            // Thời gian: phân bổ từ tháng 1 đến tháng 12/2026
            LocalDateTime batDau = LocalDateTime.of(2026, 1 + (i % 12), 1 + random.nextInt(27),
                    random.nextInt(24), 0, 0);
            LocalDateTime ketThuc = batDau.plusHours(1 + random.nextInt(3)); // kéo dài 1-3 giờ

            // Trạng thái: 5 đã kết thúc, 5 đang diễn ra, 10 sắp diễn ra
            SaleStatus trangThai;
            if (i <= 5) trangThai = SaleStatus.DA_KET_THUC;
            else if (i <= 10) trangThai = SaleStatus.DANG_DIEN_RA;
            else trangThai = SaleStatus.SAP_DIEN_RA;

            int giamGia = 30 + random.nextInt(41); // 30-70%

            ds.add(new FlashSaleEvent(
                    String.format("EVT-%03d", i),
                    ten,
                    batDau.format(DATETIME_FMT),
                    ketThuc.format(DATETIME_FMT),
                    trangThai,
                    giamGia
            ));
        }
        return ds;
    }

    /** Sinh danh sách sản phẩm Flash Sale (flash_items) */
    private static List<FlashSaleItem> sinhFlashItem(int soLuong,
                                                      List<FlashSaleEvent> events,
                                                      List<Product> products) {
        System.out.printf("  🔄 Đang sinh %,d Flash Sale Items...%n", soLuong);
        List<FlashSaleItem> ds = new ArrayList<>();

        for (int i = 1; i <= soLuong; i++) {
            FlashSaleEvent event = events.get(random.nextInt(events.size()));
            Product product = products.get(random.nextInt(products.size()));

            int gioiHan = 5 + random.nextInt(96);  // 5 - 100
            int daBan = 0; // Chưa bán, sẽ bán trong simulation
            double giaFlash = lamTronGia(product.getOriginalPrice()
                    * (100 - event.getDiscountPercent()) / 100.0);

            ds.add(new FlashSaleItem(
                    String.format("FSI-%05d", i),
                    event.getEventId(),
                    product.getProductId(),
                    gioiHan, daBan, giaFlash, 1
            ));
        }
        return ds;
    }

    /** Sinh danh sách đơn hàng */
    private static List<Order> sinhDonHang(int soLuong,
                                           List<Customer> customers,
                                           List<FlashSaleEvent> events) {
        System.out.printf("  🔄 Đang sinh %,d đơn hàng...%n", soLuong);
        List<Order> ds = new ArrayList<>();
        OrderStatus[] statuses = OrderStatus.values();

        for (int i = 1; i <= soLuong; i++) {
            Customer customer = customers.get(random.nextInt(customers.size()));
            FlashSaleEvent event = events.get(random.nextInt(events.size()));

            LocalDateTime thoiGian = LocalDateTime.of(2026, 1 + random.nextInt(12),
                    1 + random.nextInt(27), random.nextInt(24),
                    random.nextInt(60), random.nextInt(60));

            OrderStatus trangThai = statuses[random.nextInt(statuses.length)];
            double tongTien = lamTronGia(50000 + random.nextDouble() * 9950000); // tạm tính

            ds.add(new Order(
                    String.format("ORD-%05d", i),
                    customer.getCustomerId(),
                    event.getEventId(),
                    thoiGian.format(DATETIME_FMT),
                    trangThai,
                    tongTien
            ));
        }
        return ds;
    }

    /** Sinh danh sách chi tiết đơn hàng */
    private static List<OrderDetail> sinhChiTietDonHang(int soLuong,
                                                         List<Order> orders,
                                                         List<FlashSaleItem> flashItems) {
        System.out.printf("  🔄 Đang sinh %,d chi tiết đơn hàng...%n", soLuong);
        List<OrderDetail> ds = new ArrayList<>();

        for (int i = 1; i <= soLuong; i++) {
            Order order = orders.get(random.nextInt(orders.size()));
            FlashSaleItem item = flashItems.get(random.nextInt(flashItems.size()));

            int soLuongMua = 1 + random.nextInt(2); // 1 hoặc 2 (tối đa 2)
            double donGia = item.getFlashPrice();

            ds.add(new OrderDetail(
                    String.format("DTL-%05d", i),
                    order.getOrderId(),
                    item.getFlashItemId(),
                    soLuongMua,
                    donGia
            ));
        }
        return ds;
    }

    // ======================== GHI FILE CSV ========================

    /** Ghi danh sách entity ra file CSV với encoding UTF-8 */
    private static <T extends BaseEntity> void ghiFile(String filePath, List<T> danhSach) {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {

            // Ghi BOM cho Excel đọc đúng tiếng Việt
            writer.write('\uFEFF');

            // Ghi dòng tiêu đề
            if (!danhSach.isEmpty()) {
                writer.write(danhSach.get(0).getCsvHeader());
                writer.newLine();
            }

            // Ghi từng dòng dữ liệu
            for (T entity : danhSach) {
                writer.write(entity.toCsvLine());
                writer.newLine();
            }

            System.out.printf("  ✅ Đã ghi: %s (%,d dòng)%n", filePath, danhSach.size());

        } catch (IOException e) {
            System.err.printf("  ❌ Lỗi ghi file %s: %s%n", filePath, e.getMessage());
        }
    }

    // ======================== KIỂM TRA ROUND-TRIP ========================

    /** Kiểm tra parse/serialize round-trip cho tất cả entity */
    private static void kiemTraRoundTrip(List<Product> products, List<Customer> customers,
                                          List<FlashSaleEvent> events, List<FlashSaleItem> flashItems,
                                          List<Order> orders, List<OrderDetail> orderDetails) {
        System.out.println("🔍 KIỂM TRA PARSE/SERIALIZE ROUND-TRIP:");
        System.out.println("  ─────────────────────────────────────");

        int tongLoi = 0;

        // Kiểm tra Product
        tongLoi += kiemTraEntity("Product", products, new Product());
        // Kiểm tra Customer
        tongLoi += kiemTraEntity("Customer", customers, new Customer());
        // Kiểm tra FlashSaleEvent
        tongLoi += kiemTraEntity("FlashSaleEvent", events, new FlashSaleEvent());
        // Kiểm tra FlashSaleItem
        tongLoi += kiemTraEntity("FlashSaleItem", flashItems, new FlashSaleItem());
        // Kiểm tra Order
        tongLoi += kiemTraEntity("Order", orders, new Order());
        // Kiểm tra OrderDetail
        tongLoi += kiemTraEntity("OrderDetail", orderDetails, new OrderDetail());

        System.out.println("  ─────────────────────────────────────");
        if (tongLoi == 0) {
            System.out.println("  ✅ TẤT CẢ ĐỀU PASS — Parse/Serialize round-trip chính xác!");
        } else {
            System.out.printf("  ❌ CÓ %d LỖI — Cần kiểm tra lại!%n", tongLoi);
        }
    }

    /** Kiểm tra round-trip cho một loại entity (lấy 100 mẫu) */
    private static <T extends BaseEntity> int kiemTraEntity(String tenEntity,
                                                             List<T> danhSach, T mauTrong) {
        int soLoi = 0;
        int soMau = Math.min(100, danhSach.size());

        for (int i = 0; i < soMau; i++) {
            T goc = danhSach.get(i);
            String csvLine = goc.toCsvLine();

            try {
                // Tạo instance mới và parse từ CSV
                @SuppressWarnings("unchecked")
                T parsed = (T) goc.getClass().getDeclaredConstructor().newInstance();
                parsed.fromCsvLine(csvLine);

                String csvSauParse = parsed.toCsvLine();

                if (!csvLine.equals(csvSauParse)) {
                    System.out.printf("  ❌ %s[%d] KHÁC: %n     Gốc : %s%n     Parse: %s%n",
                            tenEntity, i, csvLine, csvSauParse);
                    soLoi++;
                }
            } catch (Exception e) {
                System.out.printf("  ❌ %s[%d] LỖI PARSE: %s%n", tenEntity, i, e.getMessage());
                soLoi++;
            }
        }

        if (soLoi == 0) {
            System.out.printf("  ✅ %s: %d/%d mẫu PASS%n", tenEntity, soMau, soMau);
        } else {
            System.out.printf("  ❌ %s: %d/%d mẫu LỖI%n", tenEntity, soLoi, soMau);
        }
        return soLoi;
    }

    // ======================== HÀM TIỆN ÍCH ========================

    /** Mã chữ viết tắt cho danh mục (dùng tạo tên sản phẩm duy nhất) */
    private static String maoChu(ProductCategory cat) {
        switch (cat) {
            case DIEN_TU: return "DT";
            case THOI_TRANG: return "TT";
            case GIA_DUNG: return "GD";
            case LAM_DEP: return "LD";
            case THUC_PHAM: return "TP";
            case THE_THAO: return "TH";
            default: return "XX";
        }
    }

    /** Làm tròn giá đến hàng nghìn VNĐ */
    private static double lamTronGia(double gia) {
        return Math.round(gia / 1000.0) * 1000.0;
    }

    /** Bỏ dấu tiếng Việt (dùng cho email) */
    private static String boDau(String str) {
        String result = str;
        result = result.replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a");
        result = result.replaceAll("[èéẹẻẽêềếệểễ]", "e");
        result = result.replaceAll("[ìíịỉĩ]", "i");
        result = result.replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o");
        result = result.replaceAll("[ùúụủũưừứựửữ]", "u");
        result = result.replaceAll("[ỳýỵỷỹ]", "y");
        result = result.replaceAll("[đ]", "d");
        result = result.replaceAll("[ÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴ]", "A");
        result = result.replaceAll("[ÈÉẸẺẼÊỀẾỆỂỄ]", "E");
        result = result.replaceAll("[ÌÍỊỈĨ]", "I");
        result = result.replaceAll("[ÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠ]", "O");
        result = result.replaceAll("[ÙÚỤỦŨƯỪỨỰỬỮ]", "U");
        result = result.replaceAll("[ỲÝỴỶỸ]", "Y");
        result = result.replaceAll("[Đ]", "D");
        return result;
    }
}
