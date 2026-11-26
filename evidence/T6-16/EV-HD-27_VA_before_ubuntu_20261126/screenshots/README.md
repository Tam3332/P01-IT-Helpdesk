# Ảnh console Ubuntu gốc

Hai PNG được chụp framebuffer VM Ubuntu Server bằng VBoxManage. Console đọc và in tổng hợp từ JSON thực sau lượt chạy; không phải ảnh ZAP GUI hoặc Kali. Thời điểm chụp nằm trên ảnh và khác thời điểm request trong manifest. ẢnhHTTP có fontconsole không hiển thị đủ dấu tiếngViệt ở thông báo login; dữ liệu UTF8 nguyên vẹn ở http/*.json. Không ghép, vẽ lại hoặc sửa ảnh.

## Ảnh chi tiết bổ sung

Bổ sung ảnh chụp ngày 26/11/2026 để Khiêm duyệt. 37 ảnh gốc: 25 ảnh cho 20 dòng HTTP (17 mã ca; gồm 5 ảnh chi tiết bổ sung), 8 ảnh từ 5 report ZAP (gồm CSP/SRI/HSTS), 4 ảnh kiểm tra role.

Ảnh ZAP mở report HTML gốc đã sanitize trên Edge. Ảnh HTTP mở trang xem JSON đã lưu, có nhãn trên từng trang; không phải ảnh ZAP desktop/Postman hoặc lượt chạy mới. Kết quả và thời điểm request gốc giữ nguyên. `capture-manifest-detail.json` ghi nguồn, SHA256, thời điểm chụp và kích thước từng ảnh. Giữ nguyên byte JPEG do CUA trả về, không ghép hoặc sửa nội dung.

Xem [mục lục](../Screenshot_Index.md), [gallery](../Screenshot_Gallery.html), [manifest](../capture-manifest-detail.json). Hai ảnh framebuffer Ubuntu phía trên được giữ nguyên.
