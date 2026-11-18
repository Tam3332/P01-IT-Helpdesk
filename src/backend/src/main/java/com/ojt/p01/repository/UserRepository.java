package com.ojt.p01.repository;

import com.ojt.p01.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"role"})
    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = {"role"})
    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

 /** Tài khoản đang hoạt động theo role, xếp theo tên (dùng cho GET /users/it-staff). */
    List<User> findByRole_RoleNameAndIsActiveTrueOrderByFullNameAsc(String roleName);

    @Override
    @EntityGraph(attributePaths = {"role"})
    Page<User> findAll(Pageable pageable);
}
