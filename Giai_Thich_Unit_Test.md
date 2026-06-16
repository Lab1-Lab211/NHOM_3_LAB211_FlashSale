# Giải thích chi tiết các hàm kiểm thử (Unit Test)

Tài liệu này giải thích từng dòng code của 3 phương thức quan trọng trong `RepositoryTest.java`: `testProductRepository`, `testCustomerRepository` và `testPerformance`.

## 1. Phương thức `testProductRepository`

Phương thức này kiểm tra các tính năng CRUD và tìm kiếm của Kho chứa Sản phẩm (ProductRepository).

```java
public void testProductRepository() {
    // Khởi tạo đối tượng ProductRepository và nạp dữ liệu từ file products.csv
    ProductRepository repo = new ProductRepository("data/products.csv");

    // Lấy toàn bộ danh sách sản phẩm
    List<Product> all = repo.findAll();
    // Khẳng định danh sách trả về không được trống
    assertFalse(all.isEmpty(), "findAll() phải đọc được dữ liệu");
    // Khẳng định số lượng sản phẩm đọc được phải đúng 5000 dòng
    assertEquals(5000, all.size(), "findAll() đọc đúng 5000 dòng");

    // Tìm kiếm sản phẩm có mã ID là "PRD-00001"
    Optional<Product> first = repo.findById("PRD-00001");
    // Khẳng định chắc chắn phải tìm thấy sản phẩm này
    assertTrue(first.isPresent(), "findById('PRD-00001') tìm thấy");
    
    // Nếu tìm thấy, thực hiện chuỗi kiểm tra các thuộc tính của sản phẩm đó
    first.ifPresent(p -> {
        // Khẳng định mã sản phẩm phải chính xác là "PRD-00001"
        assertEquals("PRD-00001", p.getProductId(), "productId đúng");
        // Khẳng định sản phẩm phải có tên (không được null)
        assertNotNull(p.getName(), "có tên sản phẩm");
        // Khẳng định tên sản phẩm không được là chuỗi rỗng
        assertFalse(p.getName().isEmpty(), "có tên sản phẩm");
        // Khẳng định giá bán nguyên thủy của sản phẩm phải lớn hơn 0
        assertTrue(p.getOriginalPrice() > 0, "giá > 0");
    });

    // Lọc danh sách các sản phẩm thuộc danh mục Điện tử (DIEN_TU)
    List<Product> dienTu = repo.findByCategory(ProductCategory.DIEN_TU);
    // Khẳng định danh sách trả về không bị trống
    assertFalse(dienTu.isEmpty(), "findByCategory(DIEN_TU) có kết quả");
    // Quét toàn bộ danh sách, khẳng định mọi sản phẩm đều có Category là DIEN_TU
    assertTrue(dienTu.stream().allMatch(p -> p.getCategory() == ProductCategory.DIEN_TU), "tất cả đều DIEN_TU");

    // Lọc danh sách các sản phẩm có mức giá từ 100.000 đến 500.000
    List<Product> priceRange = repo.findByPriceRange(100_000, 500_000);
    // Khẳng định có sản phẩm trong mức giá này
    assertFalse(priceRange.isEmpty(), "findByPriceRange có kết quả");
    // Quét toàn bộ danh sách, khẳng định mọi sản phẩm đều có giá nằm trong đoạn [100.000, 500.000]
    assertTrue(priceRange.stream().allMatch(p -> p.getOriginalPrice() >= 100_000 && p.getOriginalPrice() <= 500_000), "trong khoảng giá");

    // Lọc danh sách sản phẩm có chứa từ khóa "Tai nghe" trong tên
    List<Product> searchName = repo.findByName("Tai nghe");
    // Khẳng định tìm kiếm có trả về kết quả
    assertFalse(searchName.isEmpty(), "findByName có kết quả");

    // Lọc danh sách các sản phẩm còn hàng tồn kho (stock > 0)
    List<Product> inStock = repo.findInStock();
    // Khẳng định danh sách trả về không rỗng
    assertFalse(inStock.isEmpty(), "findInStock có kết quả");
}
```

## 2. Phương thức `testCustomerRepository`

Phương thức này kiểm tra các tính năng CRUD và tìm kiếm của Kho chứa Khách hàng (CustomerRepository).

```java
public void testCustomerRepository() {
    // Khởi tạo đối tượng CustomerRepository và nạp dữ liệu từ file customers.csv
    CustomerRepository repo = new CustomerRepository("data/customers.csv");

    // Lấy toàn bộ danh sách khách hàng
    List<Customer> all = repo.findAll();
    // Khẳng định danh sách trả về không được trống
    assertFalse(all.isEmpty());
    // Khẳng định số lượng khách hàng đọc được phải đúng 2000 dòng
    assertEquals(2000, all.size());

    // Tìm kiếm khách hàng có mã ID là "CUS-00001"
    Optional<Customer> first = repo.findById("CUS-00001");
    // Khẳng định chắc chắn phải tìm thấy khách hàng này
    assertTrue(first.isPresent());

    // Lọc danh sách các khách hàng có hạng là VIP
    List<Customer> vips = repo.findByTier(CustomerTier.VIP);
    // Khẳng định có khách hàng VIP trong dữ liệu
    assertFalse(vips.isEmpty());
    // Khẳng định tất cả các khách hàng trong danh sách trả về đều đúng là hạng VIP
    assertTrue(vips.stream().allMatch(c -> c.getTier() == CustomerTier.VIP));

    // Nếu danh sách khách hàng ban đầu không trống
    if (!all.isEmpty()) {
        // Lấy địa chỉ email của khách hàng đầu tiên trong danh sách
        String email = all.get(0).getEmail();
        // Dùng địa chỉ email đó để gọi hàm tìm kiếm và khẳng định hàm tìm kiếm hoạt động thành công
        assertTrue(repo.findByEmail(email).isPresent());
    }
}
```

## 3. Phương thức `testPerformance`

Đây là bài kiểm tra Hiệu năng siêu quan trọng nhằm chứng minh phần mềm xử lý dữ liệu cực nhanh.

```java
public void testPerformance() {
    // Đánh dấu mốc thời gian bắt đầu cho CẢ QUÁ TRÌNH
    long start = System.currentTimeMillis();
    
    // Đọc liên tục 3 kho dữ liệu từ ổ cứng lên RAM
    List<Product> products = new ProductRepository("data/products.csv").findAll(); // Đọc 5000 dòng
    List<Customer> customers = new CustomerRepository("data/customers.csv").findAll(); // Đọc 2000 dòng
    List<Order> orders = new OrderRepository("data/orders.csv").findAll(); // Đọc 2500 dòng

    // Tính TỔNG thời gian đã trôi qua
    long totalElapsed = System.currentTimeMillis() - start;

    // Tính tổng số lượng dòng đã đọc
    int totalLines = products.size() + customers.size() + orders.size();

    // Khẳng định tổng số lượng bản ghi phần mềm đọc được đạt trên 9500 dòng (xấp xỉ 10K)
    assertTrue(totalLines >= 9500, "Tổng đọc >= 9500 dòng");
    
    // CHUẨN NHẤT: Khẳng định TỔNG thời gian của cả 3 thao tác phải < 1000ms (1 giây)
    // Nếu tổng thời gian > 1s, test case sẽ đánh Fail
    assertTrue(totalElapsed < 1000, "Tổng thời gian đọc " + totalLines + " dòng là " + totalElapsed + "ms (Phải < 1000ms)");
}
```
