# Group2 P01 — Bộ hồ sơ chuẩn bị Review 1

Phiên bản v1.1, ngày 29/10/2026. Trạng thái: **Draft / Chờ Review 1**.
Người tổng hợp: Nguyễn Trọng Khiêm (SE192700).

## Mở và dùng theo thứ tự

1. Đọc `Group2_Kich_ban_va_Bien_ban_Review1.docx`: cách trình bày và mẫu biên bản trang 4.
2. Trình bày `Group2_Review1_7_slides.pptx` hoặc bản PDF 7 trang. Thời lượng khoảng 7 phút là đề xuất nội bộ; GV quyết định thực tế.
3. Mở 01 SRS, 02 SecRS, 03 SDD và 08 để đối chiếu theo câu hỏi của reviewer.
4. Chốt PC-01…PC-06 cùng người phụ trách; ghi quyết định, người xác nhận, ngày và action/owner/hạn vào 08 §4–5.
5. Sau review, sửa hồ sơ theo quyết định thực tế và xác nhận đầu ra trước khi chuyển Done.

## Nguồn đang làm việc

- [01 SRS](https://docs.google.com/document/d/1iGDgs5MjEbOUycZbdVMG-zL4vRov60f5/edit)
- [02 SecRS](https://docs.google.com/document/d/178ulBVk9rQASXX3_5tPjoc1DTZG4iGUeREI69dD-xMw/edit)
- [03 SDD](https://docs.google.com/document/d/1Wj4q4_IUBI4LX74SAq-ScLbN66R9EYP68d5IKZJEBuY/edit)
- [08 Project Management & Contribution](https://docs.google.com/document/d/1tYsp1Omfkrt7lsZ0VuTwTnsHArhhEbWn/edit)
- [Slide Google Slides](https://docs.google.com/presentation/d/11w4esdm_zuj0PYs1cGI7NfyCVXsg97LHtxTuOU-X8oo/edit)
- [Kịch bản và mẫu biên bản](https://docs.google.com/document/d/1TOlMs89dbZ6zPAbdN40wzpezLagG0fOU/edit)
- [Thư mục Review 1](https://drive.google.com/drive/folders/1o5670jSSJyzpq_LQEqLiZiqBLDQ1X5at)
- [Sheet phân công tuần 2](https://docs.google.com/spreadsheets/d/18NX_o1GiopEFlLNYtmjXm7pl4dlEY1cn7u21AKC8k7U/edit?gid=1004294130)
- [Board](https://github.com/users/Tam3332/projects/1)
- [Thẻ T2-01](https://github.com/users/Tam3332/projects/1?pane=issue&itemId=287134472)
- [DFD v0.2 nguồn draw.io](https://drive.google.com/file/d/1a5VKwU11xcttNKSt6i4QC-H8TxglubnN/view)
- [DFD v0.2 ảnh PNG](https://drive.google.com/file/d/1BCF3r5VkqwVpopUY9VL4VzQ0Zv_S4pzP/view)

Các file trong ZIP là snapshot đã xuất ngày 29/10/2026. Khi làm tiếp, dùng các link nguồn ở trên để tránh sửa nhầm bản cũ.

## Các quyết định còn chờ

| Điểm | Nội dung | Phối hợp |
|---|---|---|
| PC-01 | Stream/signed URL; TTL và thu hồi quyền truy cập tệp | Nhi, Cường/Trí, Khánh khi liên quan quyền |
| PC-02 | Reopened/state, BPMN và SLA sau mở lại | Trí, Cường, Khánh |
| PC-03 | Quyền/mã lỗi, Manager đổi status, Admin tải byte và ghi chú nội bộ | Khánh, Cường/Trí, Chính khi ảnh hưởng UI |
| PC-04 | Schema log, tool, rule, retention và quyền đọc/ghi | Hoà, Backend, Nhi |
| PC-05 | Network/TLS/ports/volumes/secrets/backup và triển khai thực tế | Nhi, Chính, Trí |
| PC-06 | SE review việc chuẩn hoá tham chiếu NFR/test đã sửa | Chính, Cường/Trí |

Khiêm tổng hợp quyết định và cập nhật hồ sơ. Mapping SDD §8 không thay đầu việc deployment T2-04 của Nhi.

## Minh chứng tài liệu và kiểm thử

- EV-DOC-W2-01: 01–02–03 v1.1, header và tham chiếu đã đối chiếu.
- EV-DOC-W2-02: DFD v0.2 + SecRS §3–4, mapping STRIDE.
- EV-DOC-W2-03: SDD §8, bảng Component/API–Owner–Controls–SR/NFR.
- EV-DOC-W2-04: file 08 §1–5, RACI/tracker/contribution/risk/review.
- EV-DOC-W2-05: 7 slide + kịch bản + mẫu biên bản chưa ký.

EV-HD-21…EV-HD-28 là vị trí dự kiến cho **kết quả kiểm thử thực tế**, không thay bằng bộ tài liệu này. Chưa chạy SEC-TC/pentest, chưa có PASS hoặc residual risk được xác nhận.

T2-01: In progress, còn review nhóm/GV và scope acceptance. T2-08: Review, chờ các owner xác nhận thiết kế liên quan. Chưa ghi góp ý GV hoặc chữ ký/xác nhận chưa xảy ra. Không coi nội dung đã có là bằng chứng cá nhân đã hoàn tất toàn bộ đầu việc.

Đối chiếu trên main tại commit bae236430c618b34f42a504de5cd1d765e8bdf3b: DFD vẫn v0.1. FE có ở PR #2 đang mở, chưa gộp. Những nguồn chưa tìm thấy chỉ được ghi “chưa thấy trong nguồn đã rà”, không kết luận thành viên chưa làm.
