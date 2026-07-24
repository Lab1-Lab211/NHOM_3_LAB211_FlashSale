# LAB211 – NHÓM 3

---

## Thành viên

| Tên thành viên | Vai trò / MSSV / Task week 1|
| ------------------- | --------------------------------------- |
| Võ Khả Nam | Leader / QE200059 / Datagenerator, CSV |
| Phạm Bửu Thịnh | Member / QE200023 / Usecase diagram, File description |
| Trần Hoàng Thịnh | Member / QE200056 / Class diagram, Schema |
| Hà Văn Sang | Member / DE201077 / Class diagram, Schema |

## Project

- E-Commerce Flash Sale Simulation


---

## Compile và test

Yêu cầu: JDK 8+ và Maven 3.8+.

```bash
mvn clean test
```

Các acceptance test quan trọng:

- `SynchronizationTest`: chạy 5–20 thread cùng lúc và kiểm tra invariant kho cho FILE_LOCK, SYNCHRONIZED, OPTIMISTIC.
- `SimulatorServiceTest`: kiểm tra 4 mechanism, transaction log, bảng so sánh baseline và file tổng hợp.

## Chạy DataGenerator

```bash
mvn -q -DskipTests compile
java -cp target/classes util.DataGenerator
```

## Chạy chương trình

```bash
mvn -q -DskipTests compile
java -cp target/classes view.MainView
```

Simulator ghi hai file:

- `data/transactions.csv`: chi tiết từng request/thread.
- `data/simulation_results.csv`: kết quả tổng hợp, TPS, lost update, tỷ lệ vi phạm, phần trăm so với baseline và PASS/FAIL mục tiêu.

## Tài khoản mẫu

Tài khoản do `DataGenerator` sinh sử dụng mật khẩu mặc định `Flash@123`.
Tài khoản tạo từ giao diện được tự chọn mật khẩu tối thiểu 6 ký tự. CSV chỉ lưu
PBKDF2 hash và salt, không lưu mật khẩu gốc.

Hệ thống có ba cổng role khi khởi động: Người mua, Người bán và Admin.
Người bán tự đăng ký/đăng nhập; tài khoản được lưu an toàn trong `data/sellers.csv`.
Tài khoản Admin cố định (không lưu CSV):

```text
Email: admin@gmail.com
Password: 123456
```

Flash Sale do người bán tạo có trạng thái `SAP_DIEN_RA` và tự chuyển trạng thái
theo thời gian bắt đầu/kết thúc. Admin có thể xem, kết thúc sớm sự kiện đang chạy
và chạy Simulator bốn cơ chế lock.

## Cơ chế Lock

| Mechanism     | Mô tả                        |
| ------------- | ---------------------------- |
| NO_LOCK       |Không dùng khóa, các luồng truy cập tự do nên tốc độ nhanh nhất nhưng dễ gây xung đột và sai lệch dữ liệu.     |
| FILE_LOCK     |Khóa cấp độ tệp tin để ngăn các tiến trình khác nhau cùng chỉnh sửa một file vào cùng một thời điểm. |
| SYNCHRONIZED  |Chỉ cho phép duy nhất một luồng thực thi một đoạn mã tại một thời điểm để bảo vệ an toàn dữ liệu.  |
| OPTIMISTIC    |Không khóa khi đọc mà chỉ kiểm tra phiên bản lúc ghi, giúp tối ưu hiệu năng cho hệ thống ít xảy ra tranh chấp. |
