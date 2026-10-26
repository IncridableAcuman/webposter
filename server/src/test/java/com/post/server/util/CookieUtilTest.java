package com.post.server.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class CookieUtilTest {

    private CookieUtil cookieUtil;
    private MockHttpServletResponse response;
    private final long refreshTime = 604800000L; // 7 days in ms

    @BeforeEach
    void setUp() {
        cookieUtil = new CookieUtil();
        ReflectionTestUtils.setField(cookieUtil, "refreshTime", refreshTime);
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("addCookie adds Set-Cookie header with refreshToken, httpOnly, maxAge and Lax")
    void testAddCookie() {
        String token = "sample-refresh-token";
        cookieUtil.addCookie(token, response);

        String setCookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookieHeader);
        assertTrue(setCookieHeader.contains("refreshToken=" + token));
        assertTrue(setCookieHeader.contains("Path=/"));
        assertTrue(setCookieHeader.contains("Max-Age=" + (refreshTime / 1000)));
        assertTrue(setCookieHeader.contains("HttpOnly"));
        assertTrue(setCookieHeader.contains("SameSite=Lax"));
    }

    @Test
    @DisplayName("addCookie with custom sameSite sets configured SameSite attribute")
    void testAddCookieCustomSameSite() {
        ReflectionTestUtils.setField(cookieUtil, "sameSite", "None");
        String token = "sample-refresh-token";
        cookieUtil.addCookie(token, response);

        String setCookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookieHeader);
        assertTrue(setCookieHeader.contains("SameSite=None"));
    }

    @Test
    @DisplayName("clearCookie adds Set-Cookie header with empty value and Max-Age=0")
    void testClearCookie() {
        cookieUtil.clearCookie(response);

        String setCookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookieHeader);
        assertTrue(setCookieHeader.contains("refreshToken="));
        assertTrue(setCookieHeader.contains("Max-Age=0"));
        assertTrue(setCookieHeader.contains("Path=/"));
        assertTrue(setCookieHeader.contains("HttpOnly"));
    }
}
