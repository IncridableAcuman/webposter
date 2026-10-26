package com.post.server.config;

import com.post.server.entity.UserEntity;
import com.post.server.entity.enums.Role;
import com.post.server.service.LoadUserDetailsService;
import com.post.server.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private LoadUserDetailsService loadUserDetailsService;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Claims claims;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Without Authorization header, request proceeds with no authentication")
    void testNoAuthorizationHeader() throws ServletException, IOException {
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtUtil);
        verifyNoInteractions(loadUserDetailsService);
    }

    @Test
    @DisplayName("With non-Bearer Authorization header, request proceeds with no authentication")
    void testNonBearerHeader() throws ServletException, IOException {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtUtil);
    }

    @Test
    @DisplayName("With valid ACCESS token, user is authenticated and set in SecurityContext")
    void testValidAccessToken() throws ServletException, IOException {
        String token = "valid-access-token";
        request.addHeader("Authorization", "Bearer " + token);

        UserEntity user = UserEntity.builder()
                .id(1L)
                .username("john")
                .email("john@example.com")
                .password("pass")
                .role(Role.USER)
                .build();

        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.extractClaims(token)).thenReturn(claims);
        when(claims.get("type", String.class)).thenReturn(JwtUtil.ACCESS);
        when(claims.getSubject()).thenReturn("john@example.com");
        when(loadUserDetailsService.loadUserByUsername("john@example.com")).thenReturn(user);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("john@example.com", ((UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getEmail());
    }

    @Test
    @DisplayName("With REFRESH token in Authorization header, authentication is not set")
    void testRefreshTokenIgnored() throws ServletException, IOException {
        String token = "valid-refresh-token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.extractClaims(token)).thenReturn(claims);
        when(claims.get("type", String.class)).thenReturn(JwtUtil.REFRESH);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(loadUserDetailsService);
    }

    @Test
    @DisplayName("With invalid token where validateToken is false, authentication is not set")
    void testInvalidToken() throws ServletException, IOException {
        String token = "invalid-token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.validateToken(token)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("When JwtException occurs, returns 401 and clears SecurityContext")
    void testJwtException() throws ServletException, IOException {
        String token = "corrupted-token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.validateToken(token)).thenThrow(new JwtException("Token expired"));

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Token invalid or expired"));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("When SecurityContext already has authentication, user details are not re-loaded")
    void testExistingAuthenticationNotOverwritten() throws ServletException, IOException {
        String token = "valid-token";
        request.addHeader("Authorization", "Bearer " + token);

        UsernamePasswordAuthenticationToken existingAuth =
                new UsernamePasswordAuthenticationToken("existing", null, null);
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.extractClaims(token)).thenReturn(claims);
        when(claims.get("type", String.class)).thenReturn(JwtUtil.ACCESS);
        when(claims.getSubject()).thenReturn("john@example.com");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertSame(existingAuth, SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(loadUserDetailsService);
    }
}
