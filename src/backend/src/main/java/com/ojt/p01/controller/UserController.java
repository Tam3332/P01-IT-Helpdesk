package com.ojt.p01.controller;

import com.ojt.p01.common.ApiResponse;
import com.ojt.p01.dto.response.UserResponse;
import com.ojt.p01.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API tra cứu người dùng cho nghiệp vụ (khác /admin/users của Admin).
 * Quyền kiểm ở SecurityConfig: /api/v1/users/it-staff chỉ IT_MANAGER (SDD §6.2).
 */
@Tag(name = "Users", description = "Tra cứu người dùng phục vụ phân công")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** GET /api/v1/users/it-staff: IT Staff đang hoạt động (UC-06 bước 4). 401 thiếu token, 403 không phải IT_MANAGER. */
    @Operation(summary = "Danh sách IT Staff đang hoạt động để phân công (UC-06)")
    @GetMapping("/it-staff")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getItStaff() {
        return ResponseEntity.ok(ApiResponse.success(userService.getActiveItStaff()));
    }
}
