# AI REFLECTION — Hà Văn Sang (DE201077)

## Vai trò: Member / Class Diagram, Schema

---

## 1. Quá trình sử dụng AI trong dự án

Trong dự án E-Commerce Flash Sale Simulation, tôi được phân công thiết kế Class Diagram và Schema. Đây là công việc đòi hỏi hiểu rõ mối quan hệ giữa các entity, kiểu dữ liệu, và đặc biệt là cơ chế đồng bộ (concurrency control) — vốn là điểm nhấn quan trọng nhất của dự án. Tôi đã sử dụng AI (ChatGPT, Gemini) xuyên suốt quá trình thiết kế.

### Giai đoạn 1: Thiết kế Class Diagram (Tuần 1–2)

Tôi bắt đầu bằng việc yêu cầu AI thiết kế class diagram cho hệ thống Flash Sale. Prompt đầu tiên tôi cung cấp rất tổng quát: "Thiết kế class diagram cho hệ thống e-commerce flash sale bằng Java, dùng CSV làm storage". AI trả về một diagram khá phức tạp với hơn 15 class, bao gồm cả Service layer, Controller, View, DAO pattern.

Tôi nhận ra cần giới hạn scope. Sau khi thảo luận với nhóm, tôi quyết định tập trung vào Model layer và Repository layer — đúng với cấu trúc thực tế của dự án. Prompt lần hai chi tiết hơn: "Thiết kế class diagram chỉ cho Model layer (7 entity) và Repository layer (CsvRepository generic), không dùng database, chỉ CSV".

AI lần này cho kết quả tốt hơn nhiều. Diagram bao gồm:
- `BaseEntity` (abstract) với `toCsvLine()`, `fromCsvLine()`, `getCsvHeader()`
- 7 entity con kế thừa BaseEntity
- `CsvRepository<T extends BaseEntity>` với CRUD methods
- 7 repository con

Tuy nhiên, AI bỏ sót mối quan hệ composition/aggregation giữa `FlashSaleItem` và `FlashSaleEvent` (1 event có nhiều items). Tôi phải tự thêm vào.

### Giai đoạn 2: Schema thiết kế cho Race Condition (Tuần 2–3)

Phần quan trọng nhất tôi phụ trách là thiết kế schema giải thích race condition và 4 cơ chế lock. Tôi yêu cầu AI giải thích bằng sequence diagram cách mỗi cơ chế hoạt động khi 2 thread đồng thời bán cùng một sản phẩm.

AI giải thích rất rõ ràng:
- **NO_LOCK**: Thread-1 đọc soldQty=49, Thread-2 cũng đọc 49, cả hai ghi 50 → mất 1 đơn
- **SYNCHRONIZED**: Thread-2 phải chờ Thread-1 xong mới được vào critical section
- **FILE_LOCK**: Tương tự SYNCHRONIZED nhưng ở mức file system, cross-process
- **OPTIMISTIC**: Cả hai đọc version=1, Thread-1 ghi version=2 thành công, Thread-2 thấy version≠1 → retry

Tôi dùng giải thích này làm nền tảng cho file `classdiagram_racecondittion.drawio.png` — biểu đồ minh họa race condition mà nhóm submit.

### Giai đoạn 3: Review và hỗ trợ testing (Tuần 4–5)

Tôi dùng AI để review class diagram có consistent với code thực tế không. Tôi paste code Java vào và yêu cầu AI so sánh với diagram. AI phát hiện ra một số không nhất quán:
- Diagram ghi `OrderTransaction.duration` nhưng code dùng `thoiGianXuLyMs()` (method, không phải field)
- Diagram thiếu field `errorMessage` trong `OrderTransaction`
- Return type của `findByEmail()` trong diagram là `Customer` nhưng code trả về `Optional<Customer>`

Nhờ những phát hiện này, tôi cập nhật lại diagram cho chính xác.

---

## 2. Đánh giá chất lượng output của AI

### Điểm mạnh

- **Hiểu OOP tốt:** AI phân tích inheritance, generic, abstract class rất chính xác. Khi tôi đưa code `CsvRepository<T extends BaseEntity>`, AI ngay lập tức hiểu đây là Generic Repository Pattern và vẽ diagram đúng.
- **Giải thích concurrency xuất sắc:** AI giải thích race condition, deadlock, livelock bằng ví dụ cụ thể rất dễ hiểu. Đặc biệt, AI vẽ timeline diagram cho multi-thread giúp tôi hiểu sâu hơn.
- **Mermaid/PlantUML generation:** AI sinh code diagram từ mô tả văn bản nhanh chóng, tiết kiệm thời gian vẽ tay.
- **Cross-reference detection:** Khi tôi đưa nhiều file code, AI tự phát hiện mối quan hệ FK giữa các entity.

### Điểm yếu

- **Diagram quá chi tiết:** AI có xu hướng thêm nhiều getter/setter vào class diagram, làm diagram rất rối. Tôi phải yêu cầu "chỉ hiển thị fields và business methods, bỏ getter/setter".
- **Không hiểu convention tiếng Việt:** AI không biết `coBanDuoc()` có nghĩa "có bán được" và `soLuongConLai()` có nghĩa "số lượng còn lại". AI coi đây là tên phương thức bình thường và không đưa vào mục "business logic methods" trong diagram.
- **UML notation không chuẩn:** AI đôi khi dùng notation không đúng UML chuẩn — ví dụ dùng mũi tên thực (solid arrow) cho dependency thay vì mũi tên nét đứt (dashed arrow).

---

## 3. Các lỗi AI mắc phải

1. **Sai multiplicity:** AI ghi quan hệ Customer–Order là "1..1" (mỗi customer có đúng 1 order), nhưng thực tế là "1..*" (1 customer có thể có nhiều order). Lỗi này nghiêm trọng nếu đưa vào tài liệu nộp.

2. **Nhầm inheritance với composition:** AI ban đầu vẽ `FlashSaleItem` kế thừa `Product`, nhưng thực tế `FlashSaleItem` chỉ chứa `productId` (FK reference), không phải là-một Product.

3. **Thiếu trường trong class diagram:** AI bỏ quên trường `version` trong `Product` class trên diagram, dù trong code có đầy đủ. Có thể do AI coi `version` là metadata không quan trọng.

4. **Sai sequence cho Optimistic Lock:** AI vẽ sequence diagram với chỉ 1 lần retry. Trong code thực tế, MAX_RETRY = 3, tức có thể retry tới 3 lần trước khi throw `OptimisticLockException`. Tôi phải sửa diagram để thể hiện retry loop.

5. **Notation lẫn lộn:** Trong cùng một diagram, AI dùng cả notation UML 1.x (dấu thoi cho aggregation) và UML 2.0 (dấu + cho public), tạo sự không nhất quán.

---

## 4. Bài học rút ra

1. **Diagram phải match code:** Bài học lớn nhất là class diagram PHẢI khớp 100% với code thực tế. AI giúp sinh diagram nhanh nhưng luôn có sai lệch. Quy trình đúng là: code-first → sinh diagram → so sánh → sửa diagram.

2. **Concurrency cần hiểu sâu, không dùng AI bề mặt:** AI giải thích concept tốt, nhưng khi áp dụng vào code cụ thể (ví dụ: FILE_LOCK trên Windows vs Linux), cần tự nghiên cứu thêm vì AI thiếu kiến thức về platform-specific behavior.

3. **Giữ diagram đơn giản:** Sau nhiều lần thử, tôi học được rằng diagram tốt nhất là diagram đơn giản, chỉ thể hiện những gì quan trọng. Bỏ getter/setter, bỏ private fields không quan trọng, chỉ giữ business methods và relationships.

4. **AI tốt cho brainstorming, không tốt cho precision:** Dùng AI để explore các mối quan hệ có thể có, nhưng việc xác định chính xác multiplicity, direction, và type (association/composition/aggregation) phải do người quyết định.

5. **Iterative refinement:** Không yêu cầu AI làm hoàn chỉnh 1 lần. Chia thành nhiều bước: vẽ skeleton → review → thêm details → review → finalize. Mỗi bước đều kiểm tra với code.

---

## Tổng kết

AI là công cụ hỗ trợ đắc lực cho thiết kế class diagram và giải thích concurrency concepts. Tôi ước tính tiết kiệm 35–45% thời gian nhờ AI. Tuy nhiên, sai sót về multiplicity, notation, và thiếu field buộc tôi phải review rất kỹ. Kinh nghiệm quan trọng nhất: diagram phải là reflection của code, không phải ngược lại.

Ước tính thời gian tiết kiệm: 35–45% tổng thời gian làm việc. Nhưng phải dành thêm 15–20% cho việc kiểm tra và sửa lỗi output của AI. Tổng cộng, AI giúp giảm khoảng 20–25% thời gian thực tế so với làm hoàn toàn thủ công.