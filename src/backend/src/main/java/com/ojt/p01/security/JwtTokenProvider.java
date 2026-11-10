package com.ojt.p01.security;

import com.ojt.p01.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Tạo và kiểm tra JWT (Task C2).
 * Thuật toán HS256 (HMAC-SHA256), khóa lấy từ jwt.secret (biến môi trường JWT_SECRET).
 * Payload: sub (userId), username, email, role, iat, exp, jti.
 * Hạn dùng: jwt.expiration-ms (mặc định 900000 ms = 15 phút).
 */
@Component
public class JwtTokenProvider {

    // HS256 yêu cầu khóa tối thiểu 256 bit = 32 byte
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret,
                            @Value("${jwt.expiration-ms}") long expirationMs) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_BYTES) {
            // Dừng ứng dụng ngay khi khởi động nếu khóa quá ngắn, tránh chạy với khóa yếu
            throw new IllegalStateException("JWT_SECRET phải dài tối thiểu 32 byte cho HS256");
        }
        // Gắn rõ thuật toán HmacSHA256 để token luôn ký bằng HS256 như
        this.signingKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        this.expirationMs = expirationMs;
    }

 /**
 * Tạo access token cho user đã đăng nhập thành công.
 * Role lấy từ DB (user.getRole), không lấy từ dữ liệu client gửi lên.
 */
    public String generateToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())          // jti: mã riêng của token, dùng để thu hồi khi logout
                .subject(String.valueOf(user.getId()))     // sub: userId
                .claim("username", user.getUsername())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().getRoleName())
                .issuedAt(now)                             // iat
                .expiration(expiry)                        // exp
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

 /**
 * Kiểm tra chữ ký và hạn dùng, rồi trả về phần payload (claims).
 * Token sai chữ ký / hết hạn / sai định dạng sẽ ném JwtException.
 */
    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

 /** Trả về true nếu token hợp lệ (đúng chữ ký, còn hạn). */
    public boolean validateToken(String token) {
        try {
            getClaimsFromToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

 /** Thời gian sống của token tính bằng giây, trả về cho client ở trường expiresIn. */
    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }
}
