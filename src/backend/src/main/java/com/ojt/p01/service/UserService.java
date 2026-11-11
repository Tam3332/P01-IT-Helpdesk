package com.ojt.p01.service;

import com.ojt.p01.dto.response.UserResponse;
import com.ojt.p01.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Nghiệp vụ người dùng phục vụ các màn hình khác (chưa gồm quản trị user của Admin).
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

 /**
 * Danh sách IT Staff đang hoạt động để IT Manager chọn khi phân công.
 * : "Chỉ trả IT Staff đang hoạt động". UserResponse không có password_hash.
 */
    @Transactional(readOnly = true)
    public List<UserResponse> getActiveItStaff() {
        return userRepository.findByRole_RoleNameAndIsActiveTrueOrderByFullNameAsc("IT_STAFF")
                .stream()
                .map(UserResponse::from)
                .toList();
    }
}
