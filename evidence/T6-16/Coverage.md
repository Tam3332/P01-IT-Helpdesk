# Phạm vi thực tế của lượt Before

| Run | Role | Messages | Alerts | Included_OpenAPI_GET | Excluded_write_operations |
| --- | --- | --- | --- | --- | --- |
| baseline-anon | ANON | 8 | 13 | 0 | 0 |
| api-employee | EMPLOYEE | 77 | 23 | 22 | 23 |
| api-it-staff | IT_STAFF | 76 | 23 | 22 | 23 |
| api-it-manager | IT_MANAGER | 76 | 23 | 22 | 23 |
| api-admin | ADMIN | 76 | 23 | 22 | 23 |

Bản openapi.yaml gốc có SHA256 trong từng manifest. Bản import chỉ giữ GET và đổi servers sang origin cô lập; toàn bộ path và operation bị loại được liệt kê trong manifest. Không tự lặp POST/PUT/PATCH/DELETE, upload hoặc download byte. Login/logout/change-password, tạo hai ticket và assign để kiểm data scope là thao tác có kiểm soát riêng. Active scan chỉ rule40018 mức LOW trên GET danh sách ticket, một thread, 600ms delay, 2req/s, tối đa1phút/rule và2phút/scan. Đây là lượt VA hữu hạn, không khẳng định hoàn tất mọi rule/endpoint.

Baseline dùng spider truyền thống; các luồng React sau đăng nhập và mọi ngữ cảnh XSS phía trình duyệt không được bao phủ bởi lượt này. T5-02 XSS lịch sử được giữ làm nguồn riêng. Context export chứa phạm vi; JWT được nạp qua environment của container ở runtime và được chứng minh trong role-check trước/import/sau scan. Context không tự đăng nhập nếu chỉ import độc lập; phải dùng automation cùng token mới.

Expired JWT được tạo bằng secret của DB/ứng dụng Before riêng, có đối chứng valid200, expired401. Đây là fixture kiểm tra thời hạn bằng lab secret, không phải giả mạo phiên trên môi trường khác.
