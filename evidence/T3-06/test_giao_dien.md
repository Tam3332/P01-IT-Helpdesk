# Test giao diện (FE `localhost:5173` + BE `cuong/be`) – các chức năng đã có backend

Cột **Kết quả 04/11**: ✅ đạt / ❌ lỗi / ⏸ chưa test.

> **Cập nhật 05/11:** (1) SRS §4 HD05 đổi phạm vi: *"IT Staff chỉ xem ticket được giao cho mình."* (Trí xác nhận) → C1–C3 phải test lại. (2) BE đã có thêm API đổi trạng thái `PATCH /tickets/{id}/status`, đóng ticket `POST /tickets/{id}/close`, bình luận, đính kèm, lịch sử (merge tri/be 27792af) → C4, D6 test được.
Cột **Đã phát hiện**: lỗi đã biết tính đến 04/11 17:40 và ai xử lý; "Chưa phát hiện lỗi" nghĩa là chưa thấy vấn đề (có thể chưa test).
Nguồn đối chiếu: SDD §5.1 (màn hình), §5.4 (nút theo vai trò), SRS HD01/03/05/06.

## Tài khoản test (mật khẩu chung `Demo@12345`)

| Role | Email | Tên hiển thị | Ghi chú |
|---|---|---|---|
| Employee | `employee@helpdesk.local` | Nguyễn Văn An (Kế toán) | Người tạo ticket |
| IT Staff | `staff@helpdesk.local` | Trần Thị Bình (IT) | id = 2, người được phân công |
| IT Manager | `manager@helpdesk.local` | Lê Minh Cường | Phân công, xem toàn bộ |
| Admin | `admin@helpdesk.local` | Phạm Quốc Dũng | Danh mục, SLA |

Chỉ có 1 Employee và 1 IT Staff, nên chưa test được ca cần 2 người cùng role (Employee A xem ticket của B, phân công sang Staff khác).

## Minh chứng (evidence)

File này nằm ở `evidence/T3-06/test_giao_dien.md` (nhánh `cuong/be`). Ảnh của từng ca nằm cùng thư mục:

```
evidence/T3-06/
├── README.md            ← bảng tra: mã EV → file ảnh → ca test → nội dung
├── test_giao_dien.md    ← file này (checklist + kết quả)
├── api/                 ← ảnh test API bằng PowerShell / Swagger
├── db/                  ← ảnh truy vấn PostgreSQL (kiểm dữ liệu ghi vào DB)
└── ui/                  ← ảnh chụp giao diện theo checklist này: nhóm ca → ngày test
    ├── A_General/       ← A. Chung (mọi role)
    │   └── 2026-11-04/
    ├── B_Employee/      ← B. Employee
    │   └── 2026-11-04/
    ├── C_ITStaff/       ← C. IT Staff
    │   ├── 2026-11-04/  ← phạm vi cũ (Staff thấy cả ticket Mới)
    │   └── 2026-11-05/  ← phạm vi mới (chỉ ticket được giao)
    ├── D_ITManager/     ← D. IT Manager
    │   ├── 2026-11-04/
    │   └── 2026-11-05/  ← D6 đóng ticket
    └── E_Admin/         ← E. Admin
        └── 2026-11-04/
```

Tên ảnh giao diện: `<mã EV>_ui_<mã ca>_<mô tả ngắn>.png`, ví dụ `ui/D_ITManager/2026-11-04/EV-HD-06_ui_D3_sau_phan_cong.png` là ca D3, chụp ngày 04/11, minh chứng cho EV-HD-06.
- `EV-HD-01/03/05/06`: đăng nhập / tạo ticket / xem ticket / phân công (theo SRS HD01, HD03, HD05, HD06).
- `EV-UC-10`: quản lý danh mục, SLA của Admin (SRS UC-10).

Tìm ảnh của một ca: mở `README.md`, mục "Kiểm thử giao diện theo checklist", tìm theo mã ca (A1, D3, E5…).

### Mốc ngày test

| Ngày | Bản backend (`cuong/be`) | Đã test | Ghi chú |
|---|---|---|---|
| 2026-11-04 | trước 840789b (chưa có cột Người xử lý), rồi 840789b | A, B, C1–C4, D1–D4, D7, E | Phạm vi Staff cũ: thấy ticket được giao **và** ticket Mới (SRS §4 HD05 bản cũ) |
| 2026-11-05 | 4067036 (merge tri/be 27792af) + sửa phạm vi Staff | C1–C4, D6 | SRS §4 HD05 sửa: *"IT Staff chỉ xem ticket được giao cho mình."*; đã có API đổi trạng thái, đóng ticket, bình luận, đính kèm |

Cột "Kết quả" ghi ngày trong ngoặc khi ca được test lại, ví dụ `✅ (05/11)`; ảnh nằm ở thư mục ngày tương ứng.

## A. Chung (mọi role) – SCR-01, khung giao diện

| # | Làm | Mong đợi | Nguồn | Đã phát hiện | Kết quả 04/11 |
|---|---|---|---|---|---|
| A1 | Đăng nhập sai mật khẩu, rồi sai email | Cùng 1 thông báo chung, ở lại trang login | UC-01 2a | Chưa phát hiện lỗi | ✅ |
| A2 | Bỏ trống email/mật khẩu, bấm Đăng nhập | Báo lỗi ngay tại ô, không gửi request | SCR-01 | Chưa phát hiện lỗi | ⏸ |
| A3 | Đăng nhập đúng từng role | Vào đúng trang tổng quan, menu đúng role | SCR-01 | Chưa phát hiện lỗi | ✅ (4 role, 03–04/11) |
| A4 | Đang ở trang bất kỳ, bấm F5 | Vẫn đăng nhập, ở nguyên trang | `/auth/me` | Đã sửa 04/11 (trước bị về login vì BE thiếu `/auth/me`) – BE | ✅ |
| A5 | Bấm Đăng xuất, rồi bấm nút Back trình duyệt | Về login, không xem lại được trang cũ | UC-01 6a | Đã sửa 04/11: logout thu hồi token thật – BE | ⏸ |
| A6 | Đăng nhập **Employee**, gõ thẳng vào thanh địa chỉ: `localhost:5173/admin/users`, Enter. Lặp lại với `localhost:5173/manager/tickets` | Hiện trang **403** (không có quyền), không thấy danh sách user / ticket của Manager | SDD §5 (route guard), §6.2 | Nhỏ (FE): breadcrumb trang 403 ghi "Không tìm thấy" – Chính | ✅ |
| A7 | Đăng nhập **Employee**, gõ `localhost:5173/employee/abc`, rồi `localhost:5173/employee/tickets/HD-2026-9999` (ticket không có) | Ảnh 1: trang **404**. Ảnh 2: báo không tìm thấy ticket, không lỗi trắng trang | SDD §5; HD05 (404) | Nhỏ (FE): trang 404 breadcrumb ghi "Tổng quan", menu sáng 2 mục; ticket không có vẫn hiện nút "Thử lại" – Chính | ✅ |
| A8 | Để quá 15 phút rồi bấm thao tác | Tự về login, báo hết phiên | UC-01 3a | Chưa phát hiện lỗi | ✅ |

## B. Employee – SCR-02, SCR-03, SCR-04

| # | Làm | Mong đợi | Nguồn | Đã phát hiện | Kết quả 04/11 |
|---|---|---|---|---|---|
| B1 | Tạo ticket hợp lệ | Báo thành công, chuyển sang trang chi tiết, trạng thái Mới | SCR-04 | Chưa phát hiện lỗi | ✅ |
| B2 | Tạo ticket bỏ trống tiêu đề / mô tả / danh mục | Báo lỗi tại ô, không tạo | UC-02 | Nhỏ (FE): bộ đếm tính cả dấu xuống dòng (mô tả toàn Enter vẫn ghi 4/2000) – Chính | ✅ |
| B3 | Tiêu đề > 200 ký tự, mô tả > 2000 ký tự | Bộ đếm báo vượt, không cho gửi | DTO | Chưa phát hiện lỗi | ✅ |
| B4 | Danh sách: chỉ thấy ticket của mình | Đúng ticket, đủ cột (mã, tiêu đề, người yêu cầu, danh mục, ưu tiên, trạng thái, **người xử lý**, ngày tạo) | HD05 | Đã sửa 04/11 (cột Người xử lý trước luôn hiện "Chưa phân công") – BE; Nhỏ (FE): bảng có thanh cuộn ngang, cột Ngày tạo bị cắt khi tên danh mục dài – Chính | ✅ |
| B5 | Ô **tìm kiếm** theo tiêu đề | Chỉ hiện ticket khớp | SCR-02 – BE đã bổ sung | Đã sửa 04/11 (BE trước bỏ qua `search`) – test lại đạt – BE | ✅ |
| B6 | Lọc trạng thái / ưu tiên / **danh mục** | Lọc đúng | SCR-02 – danh mục BE đã bổ sung | Đã sửa 04/11 (BE trước bỏ qua `categoryId`) – test lại đạt – BE | ✅ |
| B7 | Mở chi tiết ticket của mình | Đúng thông tin, hạn SLA, người xử lý | SCR-03 | Bình luận / lịch sử / đính kèm hiện "API chưa sẵn sàng": API chưa làm, không phải lỗi | ✅ (HD-2026-0001) |
| B8 | Trang chi tiết: không có nút Phân công | Không thấy nút | SDD §5.4 | Chưa phát hiện lỗi | ✅ |

## C. IT Staff – SCR-02, SCR-07

| # | Làm | Mong đợi | Nguồn | Đã phát hiện | Kết quả 04/11 |
|---|---|---|---|---|---|
| C1 | Danh sách của Staff | Chỉ ticket được giao cho mình, cột Người xử lý đúng (SRS §4 HD05 sửa 05/11: "IT Staff chỉ xem ticket được giao cho mình.") | HD05 | 05/11: chỉ thấy HD-0001, HD-0002 (không còn ticket Mới) | ✅ (05/11) |
| C2 | Thẻ thống kê "Ticket được giao" | Chỉ đếm ticket giao cho mình | SCR-07 | 05/11: thẻ "Ticket được giao" = 2, đúng (lỗi đếm cả ticket Mới của 04/11 không còn) | ✅ (05/11) |
| C3 | Mở ticket Mới chưa giao (gõ thẳng URL) | Từ 05/11: báo không tìm thấy (BE trả 404), không lộ nội dung | HD05 (sửa 05/11), UC-04 4a | 05/11: báo "Không tìm thấy ticket hoặc bạn không có quyền xem ticket này" (BE 404). Nhỏ (FE): vẫn hiện nút "Thử lại" – Chính | ✅ (05/11) | | ⏸ test lại |
| C4 | Không có nút Phân công; bấm "Bắt đầu xử lý" ở ticket được giao | Không thấy nút Phân công; bấm "Bắt đầu xử lý" → trạng thái Đang xử lý | SDD §5.4; UC-07 bước 2 | Không thấy nút Phân công ✅. 05/11: bấm "Bắt đầu xử lý" ở HD-0001 → Đang xử lý, lịch sử ghi "Đã phân công → Đang xử lý". Bước giải quyết đã test qua API (HD-0002) | ✅ (05/11) |

## D. IT Manager – SCR-02, SCR-05, SCR-06

| # | Làm | Mong đợi | Nguồn | Đã phát hiện | Kết quả 04/11 |
|---|---|---|---|---|---|
| D1 | Danh sách: thấy toàn bộ | Đủ ticket mọi người | HD05 | Chưa phát hiện lỗi | ✅ |
| D2 | Mở hộp thoại Phân công | Dropdown chỉ có IT Staff đang hoạt động | SCR-05, `/users/it-staff` | Chưa phát hiện lỗi | ✅ |
| D3 | Phân công ticket Mới, đổi ưu tiên | Toast thành công; trạng thái **Đã phân công**; ưu tiên + hạn SLA đổi; danh sách hiện người xử lý | UC-06 | API + giao diện đạt: Đã phân công, ưu tiên Cao, hạn = tạo + 8 giờ, toast "Đã phân công ticket", nút đổi thành "Phân công lại" | ✅ |
| D4 | Phân công lại đúng người cũ, không đổi ưu tiên | Báo "IT Staff này đang phụ trách ticket" | UC-06 3b | FE tự chặn trước khi gọi API ("Ticket đang giao cho người này…"); BE cũng chặn 422 (EV-HD-06 api) | ✅ |
| D5 | Phân công lại cho người khác | Người xử lý đổi, trạng thái giữ nguyên | UC-06 – cần thêm 1 IT Staff | Chưa test được: cần thêm 1 tài khoản IT Staff – Trí seed | ⏸ |
| D6 | Nút Phân công không hiện ở ticket Đã đóng | Không thấy | SDD §5.4 | 05/11: Employee bấm "Yêu cầu đóng ticket" ở HD-0002 (đã giải quyết) → Đã đóng, lịch sử có "Xác nhận đóng ticket"; Manager mở lại không còn nút Phân công | ✅ (05/11) |
| D7 | Đăng nhập staff sau khi phân công | Ticket hiện trong danh sách được giao | HD05 | Chưa phát hiện lỗi | ✅ |

## E. Admin – SCR-08, SCR-09

| # | Làm | Mong đợi | Nguồn | Đã phát hiện | Kết quả 04/11 |
|---|---|---|---|---|---|
| E1 | Đăng nhập Admin → Cài đặt hệ thống → Danh mục → **Thêm danh mục**: Mã `TESTUI`, Tên `Danh mục test giao diện`, ưu tiên Trung bình → Thêm. Sau đó đăng nhập Employee (tab ẩn danh), mở dropdown Danh mục | Toast "Đã thêm danh mục", danh sách thêm 1 dòng; Employee thấy danh mục mới | UC-10 A | Chưa phát hiện lỗi | ✅ |
| E2 | Thêm danh mục lần nữa với Mã `TESTUI` (trùng mã); rồi Mã `TESTUI2` + Tên `Danh mục test giao diện` (trùng tên). Mở F12 → Network xem request `categories` | Báo trùng, không tạo thêm. BE trả 400 `CATEGORY_CODE_DUPLICATE` / `CATEGORY_NAME_DUPLICATE` | UC-10 3a | Trùng mã: FE hiện đúng "Mã danh mục đã tồn tại" (lỗi "FE chờ 409" ghi trước đây có thể đã được sửa – chờ xem Network). Trùng tên: chưa test | ✅ trùng mã / ⏸ trùng tên |
| E3 | Admin bấm **Tắt** ở dòng TESTUI; Employee F5 rồi mở dropdown Danh mục | Trạng thái "Tạm ẩn", nút đổi thành Bật; Employee không còn thấy danh mục | UC-10 C | Chưa phát hiện lỗi. Ảnh Employee chụp ở ô lọc Tổng quan; dropdown form Tạo ticket nên kiểm thêm | ✅ |
| E4 | Xóa danh mục đang có ticket | Báo không xóa được | UC-10 3c | Không test được trên UI: FE ghi "Danh mục không xóa được", không có nút Xóa; BE đã có `DELETE /admin/categories/{id}` → nhóm cần thống nhất (FE thêm nút hoặc bỏ API) – Trí, Chính | ⏸ |
| E5 | Admin → Cấu hình SLA → **Sửa** 1 dòng: nhập Hạn xử lý `0` → Lưu; làm lại với `-1`. Hủy rồi F5 kiểm giá trị cũ còn nguyên | Báo lỗi, không lưu | UC-10 3b | FE chặn đúng "Hạn xử lý phải lớn hơn 0." (BE chưa kiểm qua API) | ✅ |
| E6 | Đăng nhập Admin, xem menu bên trái | Có trang danh sách ticket chỉ đọc | HD05 | **Lỗi FE**: menu Admin không có mục Ticket – Chính. Ghi chú: ô Người dùng và Bản ghi nhật ký ở Tổng quan hiện "—" (chưa có API) | ❌ |

## Ảnh cần gửi để kiểm tra

Chụp cả trang (thấy được thanh địa chỉ và tên người đăng nhập), **không** dính overlay. Gửi ảnh kèm mã ca (ví dụ "B5"); khi đưa vào `evidence/T3-06/ui/<nhóm>/<ngày test>/` sẽ đổi tên theo quy ước ở mục Minh chứng.

| Ca | Chụp gì | Để kiểm tra |
|---|---|---|
| A1 | Thông báo lỗi khi sai mật khẩu và khi sai email (2 ảnh) | Hai thông báo giống hệt nhau |
| A4 | Trang chi tiết ticket sau khi F5 | Không bị về login |
| A6 | Trang hiện ra ở 2 URL của A6 (2 ảnh) | Có chặn 403 |
| A7 | Trang hiện ra ở 2 URL của A7 (2 ảnh) | Trang 404; ticket không có thì báo rõ |
| B1 | Trang chi tiết ngay sau khi tạo ticket | Trạng thái Mới, hạn SLA |
| B2 | Form tạo ticket khi bỏ trống | Thông báo lỗi tại ô |
| B4 | Danh sách của Employee | Đủ cột, cột Người xử lý đúng |
| B5 | Danh sách khi gõ `trà sữa`, và khi gõ `abcxyz` (2 ảnh) | Tìm kiếm hoạt động |
| B6 | Danh sách khi lọc 1 danh mục | Lọc danh mục hoạt động |
| C1 | Danh sách của Staff | Đúng phạm vi |
| D2 | Hộp thoại phân công đang mở dropdown | Chỉ có IT Staff đang hoạt động |
| D3 | Danh sách + chi tiết sau khi phân công | Trạng thái, ưu tiên, hạn SLA, người xử lý |
| D4 | Thông báo khi phân công lại đúng người cũ | Hiện câu "đang phụ trách" |
| E1 | Danh sách sau khi thêm + dropdown Danh mục của Employee (2 ảnh) | Danh mục hiện ngay |
| E2 | Thông báo khi trùng mã và trùng tên, kèm F12 → Network (2 ảnh) | FE hiện câu gì, BE trả mã nào |
| E3 | Danh sách sau khi Tắt + dropdown của Employee (2 ảnh) | Danh mục bị ẩn |
| E5 | Hộp Sửa SLA khi nhập 0 và -1 (2 ảnh) | Có chặn |

Các ca khác: chỉ cần ghi ✅ / ❌ vào bảng; ca ❌ thì chụp thêm ảnh lỗi và tab **F12 → Network** của request bị lỗi.
