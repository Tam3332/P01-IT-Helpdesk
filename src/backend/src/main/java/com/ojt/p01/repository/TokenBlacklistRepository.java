package com.ojt.p01.repository;

import com.ojt.p01.entity.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, Long> {

 /** true nếu token (theo jti) đã bị thu hồi khi đăng xuất. Cột jti có index nên tra nhanh. */
    boolean existsByJti(String jti);
}
