# Chào mừng đến với Markleaf

Markleaf là cuốn sổ ghi chú Markdown yên tĩnh, ưu tiên lưu cục bộ cho Android. Ứng dụng mở nhanh, không làm phiền khi bạn viết và giữ nội dung dưới dạng văn bản thuần thuộc về chính bạn.

## Một vòng tham quan ngắn

- Mở **Trang Markdown thật đẹp** để xem không gian viết.
- Mở **Thói quen viết mỗi ngày** để xem ví dụ kiểu nhật ký.
- Mở **Tóm tắt dự án** để xem việc cần làm, liên kết và cấu trúc.
- Mở **Phản chiếu thư mục cục bộ** khi bạn muốn có tệp bên ngoài ứng dụng.

> [!TIP]
> Đây là những ghi chú bình thường. Bạn có thể sửa, xuất, chuyển vào thùng rác hoặc xóa chúng khi không còn cần đến.

#start #guide #bắt-đầu

---markleaf-note---

# Trang Markdown thật đẹp

![Trang mẫu của Markleaf](attachments/starter-note-2/markleaf-sample-cover.png)

Markdown vẫn dễ đọc khi là văn bản, rồi trở nên gọn gàng, trau chuốt trong chế độ **Xem trước**.

## Ghi chú này minh họa gì

- **Đậm**, _nghiêng_, ~~gạch ngang~~ và `mã trong dòng`
- Tiêu đề, danh sách, danh sách việc cần làm, trích dẫn, đường phân cách, khối mã, bảng, khung chú thích, chú thích cuối trang, liên kết và hình ảnh
- Tô sáng cú pháp trực tiếp khi bạn gõ

> [!NOTE]
> Chuyển giữa Sửa và Xem trước trên thanh trên cùng. Ghi chú vẫn chỉ là Markdown.

| Thành phần | Dùng để |
| --- | --- |
| `#thẻ` | sắp xếp |
| `[[Tóm tắt dự án]]` | liên kết ghi chú cục bộ |
| `![](...)` | đính kèm hình ảnh |

```kotlin
fun markleaf() = "local-first markdown"
```

Một chú thích cuối trang nhỏ giữ chi tiết ở gần mà không làm gián đoạn đoạn văn.[^1]

[^1]: Chú thích cuối trang, khung chú thích, bảng và khối mã đều được hiển thị cục bộ.

#markdown #showcase #viết

---markleaf-note---

# Thói quen viết mỗi ngày

## Trang buổi sáng

Mục tiêu không phải là viết nhiều hơn. Mục tiêu là làm cho câu đầu tiên trở nên dễ dàng.

- [x] Ghi lại một ý nghĩ
- [ ] Biến một việc cần làm thành ghi chú
- [ ] Liên kết công việc liên quan với [[Tóm tắt dự án]]

> Giữ ghi chú đủ nhỏ để bạn thật sự quay lại với nó.

## Khép lại buổi tối

Hôm nay có gì tiến triển?

1. Một quyết định hữu ích
2. Một câu hỏi còn bỏ ngỏ
3. Một việc để dành cho ngày mai

#journal #writing #nhật-ký

---markleaf-note---

# Tóm tắt dự án

Ghi chú này cho thấy Markleaf có thể chứa một dự án nhỏ mà không trở nên nặng nề.

## Kết quả

Hoàn thành một cuốn sổ mẫu gọn gàng, hướng dẫn bằng chính sự hữu ích của nó.

## Kế hoạch

- [x] Trình bày cú pháp Markdown thật đẹp
- [x] Thêm một hình ảnh đính kèm
- [ ] Thử tìm kiếm với `local-first`
- [ ] Mở liên kết ngược từ **Thói quen viết mỗi ngày**

## Ghi chú

Liên quan: [[Thói quen viết mỗi ngày]] và [[Thẻ, tìm kiếm và liên kết ngược]]

#project/markleaf #planning #dự-án

---markleaf-note---

# Thẻ, tìm kiếm và liên kết ngược

Gõ thẻ ngay trong nội dung: #project, #writing, #privacy, #local-first.

## Gợi ý tìm kiếm

Thử tìm:

- `local-first`
- `folder mirror`
- `Tóm tắt dự án`

## Liên kết ngược

Wikilink dùng cú pháp `[[Tiêu đề ghi chú]]`. Khi một ghi chú khác liên kết đến đây, Markleaf có thể hiển thị mối liên hệ đó ngay trên máy. Không cần tài khoản hay máy chủ.

Xem thêm [[Tóm tắt dự án]].

#organize #search #sắp-xếp

---markleaf-note---

# Phản chiếu thư mục cục bộ

Markleaf không cần đám mây riêng. Thay vào đó, bạn có thể chọn một thư mục và để Android hoặc công cụ đồng bộ của bạn xử lý thư mục đó.

## Điều gì sẽ diễn ra

- Markleaf ghi mỗi ghi chú thành một tệp Markdown.
- Frontmatter giữ `markleaf_id` cố định.
- Tệp đính kèm nằm cạnh các ghi chú được sao chép.
- Markleaf không bao giờ tải ghi chú của bạn lên — việc đồng bộ là của công cụ bạn chọn, không phải của ứng dụng.

## Vì sao điều này quan trọng

Ghi chú của bạn vẫn đọc được trong các công cụ Markdown khác, và việc đồng bộ vẫn do bạn quyết định.

#privacy #folder-mirror #local-first #quyền-riêng-tư
