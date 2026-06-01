# 📋 CSV Schema Document — Flash Sale Simulation

> **Dự án:** E-Commerce Flash Sale Simulation (LAB211)  
> **Phiên bản:** 1.0  
> **Ngày tạo:** 19/05/2026  
> **Tổng số file CSV:** 7 file  
> **Tổng dữ liệu sinh bởi DataGenerator:** ≥ 12,520 dòng  

---

## 📊 Tổng quan các file CSV

| # | File CSV | Entity Class | Số dòng | Khóa chính | Khóa ngoại (FK) |
|---|---|---|---|---|---|
| 1 | `products.csv` | `Product` | 5,000 | `productId` | — |
| 2 | `customers.csv` | `Customer` | 2,000 | `customerId` | — |
| 3 | `flash_events.csv` | `FlashSaleEvent` | 20 | `eventId` | — |
| 4 | `flash_items.csv` | `FlashSaleItem` | 500 | `flashItemId` | `eventId`, `productId` |
| 5 | `orders.csv` | `Order` | 2,500 | `orderId` | `customerId`, `eventId` |
| 6 | `order_details.csv` | `OrderDetail` | 2,500 | `detailId` | `orderId`, `flashItemId` |
| 7 | `transactions.csv` | `OrderTransaction` | *(sinh bởi Simulator)* | `transactionId` | `orderId` |

---

## 📐 Sơ đồ quan hệ khóa ngoại

```
products.csv ──────────────┐
                           ├──→ flash_items.csv ──────┐
flash_events.csv ──────────┤                          ├──→ order_details.csv
                           ├──→ orders.csv ───────────┘
customers.csv ─────────────┘         │
                                     └──→ transactions.csv
```

---

## 1. `products.csv` — Sản phẩm

> **Entity:** `Product` | **Số dòng:** 5,000 | **Encoding:** UTF-8 BOM

| # | Cột | Kiểu dữ liệu | Ràng buộc | Mô tả | Ví dụ |
|---|---|---|---|---|---|
| 1 | `productId` | `String` | **PK**, NOT NULL, Unique | Mã sản phẩm, format `PRD-XXXXX` | `PRD-00001` |
| 2 | `name` | `String` | NOT NULL | Tên sản phẩm (tiếng Việt) | `Nồi cơm điện GD-0001` |
| 3 | `category` | `Enum` | NOT NULL | Danh mục sản phẩm | `GIA_DUNG` |
| 4 | `originalPrice` | `double` | NOT NULL, > 0 | Giá gốc (VNĐ), làm tròn hàng nghìn | `3432000.0` |
| 5 | `stock` | `int` | NOT NULL, ≥ 0 | Số lượng tồn kho (10–1000) | `890` |
| 6 | `version` | `int` | NOT NULL, ≥ 1 | Phiên bản cho Optimistic Lock | `1` |

**Giá trị Enum `category` (ProductCategory):**

| Giá trị | Mô tả |
|---|---|
| `DIEN_TU` | Điện tử |
| `THOI_TRANG` | Thời trang |
| `GIA_DUNG` | Gia dụng |
| `LAM_DEP` | Làm đẹp |
| `THUC_PHAM` | Thực phẩm |
| `THE_THAO` | Thể thao |

**Dữ liệu mẫu:**
```csv
productId,name,category,originalPrice,stock,version
PRD-00001,Nồi cơm điện GD-0001,GIA_DUNG,3432000.0,890,1
PRD-00002,Kính mát TT-0002,THOI_TRANG,3553000.0,194,1
PRD-00003,Quạt điện GD-0003,GIA_DUNG,1875000.0,364,1
```

---

## 2. `customers.csv` — Khách hàng

> **Entity:** `Customer` | **Số dòng:** 2,000 | **Encoding:** UTF-8 BOM

| # | Cột | Kiểu dữ liệu | Ràng buộc | Mô tả | Ví dụ |
|---|---|---|---|---|---|
| 1 | `customerId` | `String` | **PK**, NOT NULL, Unique | Mã khách hàng, format `CUS-XXXXX` | `CUS-00001` |
| 2 | `name` | `String` | NOT NULL | Họ và tên (tiếng Việt) | `Nguyễn Văn An` |
| 3 | `email` | `String` | NOT NULL, Unique | Địa chỉ email (không dấu) | `an.nguyen1@email.com` |
| 4 | `tier` | `Enum` | NOT NULL | Hạng thành viên | `VIP` |
| 5 | `registeredDate` | `String` | NOT NULL | Ngày đăng ký, format `yyyy-MM-dd` | `2024-05-12` |

**Giá trị Enum `tier` (CustomerTier):**

| Giá trị | Mô tả | Độ ưu tiên | Tỷ lệ phân bổ |
|---|---|---|---|
| `VIP` | Khách VIP | 1 (cao nhất) | ~10% |
| `PREMIUM` | Khách Premium | 2 | ~20% |
| `REGULAR` | Khách thường | 3 (thấp nhất) | ~70% |

**Dữ liệu mẫu:**
```csv
customerId,name,email,tier,registeredDate
CUS-00001,Lê Thanh Minh,minh.le1@email.com,REGULAR,2025-09-08
CUS-00002,Phạm Đức Tú,tu.pham2@email.com,PREMIUM,2024-03-15
```

---

## 3. `flash_events.csv` — Sự kiện Flash Sale

> **Entity:** `FlashSaleEvent` | **Số dòng:** 20 | **Encoding:** UTF-8 BOM

| # | Cột | Kiểu dữ liệu | Ràng buộc | Mô tả | Ví dụ |
|---|---|---|---|---|---|
| 1 | `eventId` | `String` | **PK**, NOT NULL, Unique | Mã sự kiện, format `EVT-XXX` | `EVT-001` |
| 2 | `eventName` | `String` | NOT NULL | Tên sự kiện (tiếng Việt) | `Flash Sale Mùa Hè #1` |
| 3 | `startTime` | `String` | NOT NULL | Thời gian bắt đầu, format `yyyy-MM-dd'T'HH:mm:ss` | `2026-02-14T08:00:00` |
| 4 | `endTime` | `String` | NOT NULL | Thời gian kết thúc, format `yyyy-MM-dd'T'HH:mm:ss` | `2026-02-14T11:00:00` |
| 5 | `status` | `Enum` | NOT NULL | Trạng thái sự kiện | `DANG_DIEN_RA` |
| 6 | `discountPercent` | `int` | NOT NULL, 30–70 | Phần trăm giảm giá | `55` |

**Giá trị Enum `status` (SaleStatus):**

| Giá trị | Mô tả | Số lượng phân bổ |
|---|---|---|
| `DA_KET_THUC` | Đã kết thúc | 5 sự kiện |
| `DANG_DIEN_RA` | Đang diễn ra | 5 sự kiện |
| `SAP_DIEN_RA` | Sắp diễn ra | 10 sự kiện |

**Dữ liệu mẫu:**
```csv
eventId,eventName,startTime,endTime,status,discountPercent
EVT-001,Flash Sale Mùa Hè #1,2026-02-14T08:00:00,2026-02-14T11:00:00,DA_KET_THUC,55
EVT-006,Flash Deal 12.12 #6,2026-07-22T15:00:00,2026-07-22T17:00:00,DANG_DIEN_RA,42
```

---

## 4. `flash_items.csv` — Sản phẩm tham gia Flash Sale

> **Entity:** `FlashSaleItem` | **Số dòng:** 500 | **Encoding:** UTF-8 BOM  
> ⚠️ **Entity quan trọng nhất** — chứa trường `version` cho Optimistic Lock.  
> **Bất biến (Invariant):** `soldQty <= limitedQty` — KHÔNG BAO GIỜ ĐƯỢC VI PHẠM.

| # | Cột | Kiểu dữ liệu | Ràng buộc | Mô tả | Ví dụ |
|---|---|---|---|---|---|
| 1 | `flashItemId` | `String` | **PK**, NOT NULL, Unique | Mã Flash Item, format `FSI-XXXXX` | `FSI-00001` |
| 2 | `eventId` | `String` | **FK → flash_events.csv**, NOT NULL | Mã sự kiện tham gia | `EVT-005` |
| 3 | `productId` | `String` | **FK → products.csv**, NOT NULL | Mã sản phẩm gốc | `PRD-02341` |
| 4 | `limitedQty` | `int` | NOT NULL, 5–100 | Số lượng giới hạn bán ra | `50` |
| 5 | `soldQty` | `int` | NOT NULL, ≥ 0 | Số lượng đã bán (ban đầu = 0) | `0` |
| 6 | `flashPrice` | `double` | NOT NULL, > 0 | Giá Flash Sale = giá gốc × (100 − discount%) / 100 | `1716000.0` |
| 7 | `version` | `int` | NOT NULL, ≥ 1 | Phiên bản — tăng mỗi khi `soldQty` thay đổi | `1` |

**Dữ liệu mẫu:**
```csv
flashItemId,eventId,productId,limitedQty,soldQty,flashPrice,version
FSI-00001,EVT-005,PRD-02341,50,0,1716000.0,1
FSI-00002,EVT-012,PRD-00789,25,0,450000.0,1
```

---

## 5. `orders.csv` — Đơn hàng

> **Entity:** `Order` | **Số dòng:** 2,500 | **Encoding:** UTF-8 BOM

| # | Cột | Kiểu dữ liệu | Ràng buộc | Mô tả | Ví dụ |
|---|---|---|---|---|---|
| 1 | `orderId` | `String` | **PK**, NOT NULL, Unique | Mã đơn hàng, format `ORD-XXXXX` | `ORD-00001` |
| 2 | `customerId` | `String` | **FK → customers.csv**, NOT NULL | Mã khách hàng đặt hàng | `CUS-01234` |
| 3 | `eventId` | `String` | **FK → flash_events.csv**, NOT NULL | Mã sự kiện Flash Sale | `EVT-003` |
| 4 | `orderTime` | `String` | NOT NULL | Thời gian đặt, format `yyyy-MM-dd'T'HH:mm:ss` | `2026-06-15T09:30:45` |
| 5 | `status` | `Enum` | NOT NULL | Trạng thái đơn hàng | `CHO_XU_LY` |
| 6 | `totalAmount` | `double` | NOT NULL, > 0 | Tổng tiền (VNĐ) | `2450000.0` |

**Giá trị Enum `status` (OrderStatus):**

| Giá trị | Mô tả |
|---|---|
| `CHO_XU_LY` | Chờ xử lý |
| `DA_XAC_NHAN` | Đã xác nhận |
| `THAT_BAI` | Thất bại |
| `DA_HUY` | Đã hủy |

**Dữ liệu mẫu:**
```csv
orderId,customerId,eventId,orderTime,status,totalAmount
ORD-00001,CUS-01234,EVT-003,2026-06-15T09:30:45,CHO_XU_LY,2450000.0
ORD-00002,CUS-00567,EVT-008,2026-03-20T14:12:30,DA_XAC_NHAN,890000.0
```

---

## 6. `order_details.csv` — Chi tiết đơn hàng

> **Entity:** `OrderDetail` | **Số dòng:** 2,500 | **Encoding:** UTF-8 BOM

| # | Cột | Kiểu dữ liệu | Ràng buộc | Mô tả | Ví dụ |
|---|---|---|---|---|---|
| 1 | `detailId` | `String` | **PK**, NOT NULL, Unique | Mã chi tiết, format `DTL-XXXXX` | `DTL-00001` |
| 2 | `orderId` | `String` | **FK → orders.csv**, NOT NULL | Mã đơn hàng chứa chi tiết này | `ORD-00001` |
| 3 | `flashItemId` | `String` | **FK → flash_items.csv**, NOT NULL | Mã sản phẩm Flash Sale đã mua | `FSI-00042` |
| 4 | `quantity` | `int` | NOT NULL, 1–2 | Số lượng mua (tối đa 2/sản phẩm/sự kiện) | `1` |
| 5 | `unitPrice` | `double` | NOT NULL, > 0 | Đơn giá tại thời điểm mua (VNĐ) | `1716000.0` |

**Trường tính toán:** `thanhTien = quantity × unitPrice`

**Dữ liệu mẫu:**
```csv
detailId,orderId,flashItemId,quantity,unitPrice
DTL-00001,ORD-00001,FSI-00042,1,1716000.0
DTL-00002,ORD-00001,FSI-00108,2,450000.0
```

---

## 7. `transactions.csv` — Giao dịch đơn hàng *(sinh bởi Simulator)*

> **Entity:** `OrderTransaction` | **Số dòng:** *(sinh khi chạy Simulator Tool)*  
> **Encoding:** UTF-8 BOM  
> ⚠️ File này **KHÔNG** được sinh bởi `DataGenerator`, mà do `Simulator` tạo khi chạy thực nghiệm so sánh cơ chế đồng bộ.

| # | Cột | Kiểu dữ liệu | Ràng buộc | Mô tả | Ví dụ |
|---|---|---|---|---|---|
| 1 | `transactionId` | `String` | **PK**, NOT NULL, Unique | Mã giao dịch | `TXN-00001` |
| 2 | `orderId` | `String` | **FK → orders.csv**, NOT NULL | Mã đơn hàng được xử lý | `ORD-00123` |
| 3 | `mechanism` | `Enum` | NOT NULL | Cơ chế đồng bộ sử dụng | `SYNCHRONIZED` |
| 4 | `threadName` | `String` | NOT NULL | Tên thread xử lý giao dịch | `Thread-3` |
| 5 | `startTime` | `long` | NOT NULL | Thời điểm bắt đầu (nanosecond) | `123456789012345` |
| 6 | `endTime` | `long` | NOT NULL | Thời điểm kết thúc (nanosecond) | `123456789112345` |
| 7 | `success` | `boolean` | NOT NULL | Kết quả: true = thành công | `true` |
| 8 | `errorMessage` | `String` | Nullable | Thông báo lỗi (rỗng nếu thành công) | `""` |

**Giá trị Enum `mechanism` (LockMechanism):**

| Giá trị | Mô tả |
|---|---|
| `NO_LOCK` | Không khóa (Baseline) |
| `FILE_LOCK` | Khóa file (NIO FileLock) |
| `SYNCHRONIZED` | Đồng bộ hóa (synchronized) |
| `OPTIMISTIC` | Khóa lạc quan (Optimistic Lock) |

**Trường tính toán:** `thoiGianXuLyMs = (endTime − startTime) / 1,000,000.0` (mili giây)

**Dữ liệu mẫu:**
```csv
transactionId,orderId,mechanism,threadName,startTime,endTime,success,errorMessage
TXN-00001,ORD-00123,SYNCHRONIZED,Thread-3,123456789012345,123456789112345,true,
TXN-00002,ORD-00456,NO_LOCK,Thread-7,123456789212345,123456789312345,false,Hết hàng
```

---

## 📎 Phụ lục: Tổng hợp Enum

| Enum | Thuộc Entity | Các giá trị |
|---|---|---|
| `ProductCategory` | Product | `DIEN_TU`, `THOI_TRANG`, `GIA_DUNG`, `LAM_DEP`, `THUC_PHAM`, `THE_THAO` |
| `CustomerTier` | Customer | `VIP`, `PREMIUM`, `REGULAR` |
| `SaleStatus` | FlashSaleEvent | `SAP_DIEN_RA`, `DANG_DIEN_RA`, `DA_KET_THUC` |
| `OrderStatus` | Order | `CHO_XU_LY`, `DA_XAC_NHAN`, `THAT_BAI`, `DA_HUY` |
| `LockMechanism` | OrderTransaction | `NO_LOCK`, `FILE_LOCK`, `SYNCHRONIZED`, `OPTIMISTIC` |
| `PaymentMethod` | *(chưa sử dụng)* | `THE_TIN_DUNG`, `VI_DIEN_TU`, `THANH_TOAN_KHI_NHAN` |

---

## 📎 Phụ lục: Quy tắc chung

| Quy tắc | Chi tiết |
|---|---|
| **Encoding** | UTF-8 có BOM (`\uFEFF`) để Excel đọc đúng tiếng Việt |
| **Delimiter** | Dấu phẩy (`,`) |
| **Header** | Dòng đầu tiên luôn là tên cột |
| **Dòng dữ liệu** | Từ dòng 2 trở đi, mỗi dòng = 1 entity |
| **Format ngày** | `yyyy-MM-dd` (vd: `2026-05-19`) |
| **Format ngày giờ** | `yyyy-MM-dd'T'HH:mm:ss` (vd: `2026-05-19T14:30:00`) |
| **Giá tiền** | Đơn vị VNĐ, làm tròn đến hàng nghìn |
| **ID format** | Prefix + số thứ tự zero-padded (`PRD-00001`, `CUS-00001`, ...) |
| **Seed** | `Random(42)` — dữ liệu có thể tái tạo lại y hệt khi chạy lại |
