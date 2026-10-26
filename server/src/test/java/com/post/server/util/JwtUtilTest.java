package com.post.server.util;

import com.post.server.entity.UserEntity;
import com.post.server.entity.enums.Role;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private UserEntity user;
    private final String secret = "abcdefghijklmnopqrstuvwxyz_0123456789_secure_key_for_testing";
    private final long refreshTime = 604800000L; // 7 days
    private final long accessTime = 900000L;     // 15 minutes

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", secret);
        ReflectionTestUtils.setField(jwtUtil, "refreshTime", refreshTime);
        ReflectionTestUtils.setField(jwtUtil, "accessTime", accessTime);
        jwtUtil.init();

        user = UserEntity.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encoded_pass")
                .role(Role.USER)
                .build();
    }

    @Test
    @DisplayName("generateAccessToken creates valid token with correct claims")
    void testGenerateAccessToken() {
        String token = jwtUtil.generateAccessToken(user);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(jwtUtil.validateToken(token));

        assertEquals("test@example.com", jwtUtil.getSubject(token));

        Claims claims = jwtUtil.extractClaims(token);
        assertEquals(1, ((Number) claims.get("id")).longValue());
        assertEquals(Role.USER.name(), claims.get("role"));
        assertEquals(JwtUtil.ACCESS, claims.get("type"));

        Date expiration = jwtUtil.getExpiration(token);
        assertTrue(expiration.after(new Date()));
        assertFalse(jwtUtil.isTokenExpired(token));
    }

    @Test
    @DisplayName("generateRefreshToken creates valid token with REFRESH type")
    void testGenerateRefreshToken() {
        String token = jwtUtil.generateRefreshToken(user);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(jwtUtil.validateToken(token));

        assertEquals("test@example.com", jwtUtil.getSubject(token));

        Claims claims = jwtUtil.extractClaims(token);
        assertEquals(1, ((Number) claims.get("id")).longValue());
        assertEquals(JwtUtil.REFRESH, claims.get("type"));
        assertTrue(jwtUtil.getExpiration(token).after(new Date(System.currentTimeMillis() + 86400000L)));
    }

    @Test
    @DisplayName("validateToken returns false for expired token")
    void testValidateTokenExpired() {
        JwtUtil expiredJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(expiredJwtUtil, "secret", secret);
        ReflectionTestUtils.setField(expiredJwtUtil, "refreshTime", -1000L);
        ReflectionTestUtils.setField(expiredJwtUtil, "accessTime", -1000L);
        expiredJwtUtil.init();

        String expiredToken = expiredJwtUtil.generateAccessToken(user);

        assertFalse(jwtUtil.validateToken(expiredToken));
    }

    @Test
    @DisplayName("validateToken returns false for token signed with wrong key")
    void testValidateTokenInvalidSignature() {
        JwtUtil anotherJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(anotherJwtUtil, "secret", "different_secret_key_that_is_at_least_256_bits_long_12345");
        ReflectionTestUtils.setField(anotherJwtUtil, "refreshTime", refreshTime);
        ReflectionTestUtils.setField(anotherJwtUtil, "accessTime", accessTime);
        anotherJwtUtil.init();

        String tokenWithDifferentKey = anotherJwtUtil.generateAccessToken(user);

        assertFalse(jwtUtil.validateToken(tokenWithDifferentKey));
    }

    @Test
    @DisplayName("validateToken returns false for malformed token string")
    void testValidateTokenMalformed() {
        assertFalse(jwtUtil.validateToken("invalid.jwt.token"));
    }

    @Test
    @DisplayName("validateToken returns false for null or empty token")
    void testValidateTokenNullOrEmpty() {
        assertFalse(jwtUtil.validateToken(null));
        assertFalse(jwtUtil.validateToken(""));
        assertFalse(jwtUtil.validateToken("   "));
    }
}
