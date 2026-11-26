# Lịch sử author và đồng bộ main ngày 26/11

Người dùng yêu cầu sửa author bằng chỉ đạo “sửa lại đi trời”. Audit cục bộ ghi sửa hai commit và dựng lại hai merge descendants, không đổi file. Đối chiếu object cục bộ trong lượt sửa hồ sơ:

```json
[
  {
    "old": "8ca44bebbcca5437f9cd53a325cd9edc5d7f5864",
    "new": "661ac09ce72dae22f84507bc56cd69d80a6c535c",
    "old_tree": "90288d0509c03e8fe36a4627aaa15b8a9aefb389",
    "new_tree": "90288d0509c03e8fe36a4627aaa15b8a9aefb389",
    "tree_identical": true
  },
  {
    "old": "5756fbad5f5fc70aa5a0af14fc4bb4f341b92ce0",
    "new": "36b7d667c601dc260da56a1b1af83b25fcdfdf87",
    "old_tree": "2ebdf8255501f82a9feb4754514485da3569f8f4",
    "new_tree": "2ebdf8255501f82a9feb4754514485da3569f8f4",
    "tree_identical": true
  },
  {
    "old": "010ade282e3774514a26e301cb1d2761d06e34dc",
    "new": "34c508ca6961cf0c779dd0f4ac70bca6566d0de5",
    "old_tree": "2ebdf8255501f82a9feb4754514485da3569f8f4",
    "new_tree": "2ebdf8255501f82a9feb4754514485da3569f8f4",
    "tree_identical": true
  },
  {
    "old": "e92bea73a87f48fd2e916b6918439542b396c397",
    "new": "c27b2529810926295ad01a2a4c680b2e43dee2fe",
    "old_tree": "d6fdccfa85b93030c073fcf1eb7becd304d9293a",
    "new_tree": "d6fdccfa85b93030c073fcf1eb7becd304d9293a",
    "tree_identical": true
  }
]
```

Commit cũ có thể không còn truy cập qua GitHub. Đây là nguồn đối chiếu cục bộ, không yêu cầu người đọc tin rằng URL commit cũ còn hoạt động. Sau đó main có hai commit trực tiếp chỉnh cách trình bày evidence: 35fec96 và c61ccb2. Các thay đổi đó không xác nhận deploy hay kết quả kiểm thử mới.

Nhóm fetch origin và đối chiếu main trước khi đồng bộ. Sao lưu thay đổi chưa commit, giữ nhánh/commit riêng và phối hợp Trí/Chính xử lý lệch lịch sử. Không tự reset --hard hoặc force-push. Tin thông báo cho nhóm là bản nháp, chưa gửi.
