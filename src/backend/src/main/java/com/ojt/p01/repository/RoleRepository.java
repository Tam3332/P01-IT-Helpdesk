package com.ojt.p01.repository;

import com.ojt.p01.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Truy vấn bảng roles (Task C1).
 * Dùng khi Admin tạo tài khoản / đổi role: tìm Role theo tên (EMPLOYEE, IT_STAFF, IT_MANAGER, ADMIN).
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {

    // Spring Data tự sinh câu SQL: SELECT * FROM roles WHERE role_name = ?
    Optional<Role> findByRoleName(String roleName);
}
