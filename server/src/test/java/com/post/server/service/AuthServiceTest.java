package com.post.server.service;

import com.post.server.dto.AuthDto;
import com.post.server.entity.TokenEntity;
import com.post.server.entity.UserEntity;
import com.post.server.entity.enums.Role;
import com.post.server.exception.CustomBadRequestException;
import com.post.server.exception.CustomNotFoundException;
import com.post.server.exception.CustomUnauthorizedException;
import com.post.server.repository.UserRepository;
import com.post.server.util.CookieUtil;
import com.post.server.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CookieUtil cookieUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Claims claims;

    @InjectMocks
    private AuthService authService;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = UserEntity.builder()
                .id(1L)
                .username("admin")
                .email("admin@gmail.com")
                .password("encoded-password")
                .role(Role.USER)
                .build();
    }

    @Test
    @DisplayName("register creates user, generates tokens, and sets cookie")
    void shouldRegisterUserSuccessfully() {
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest("admin", "admin@gmail.com", "admin2003");

        when(userRepository.existsByEmail("admin@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("admin2003")).thenReturn("encoded-password");
        when(userRepository.save(any(UserEntity.class))).thenReturn(user);
        when(jwtUtil.generateAccessToken(any(UserEntity.class))).thenReturn("mock-access-token");
        when(jwtUtil.generateRefreshToken(any(UserEntity.class))).thenReturn("mock-refresh-token");

        AuthDto.AuthResponse authResponse = authService.register(request, response);

        assertNotNull(authResponse);
        assertEquals("mock-access-token", authResponse.accessToken());
        verify(userRepository).save(any(UserEntity.class));
        verify(tokenService).saveToken(any(UserEntity.class), eq("mock-refresh-token"));
        verify(cookieUtil).addCookie("mock-refresh-token", response);
    }

    @Test
    @DisplayName("register throws CustomBadRequestException when email already exists")
    void shouldThrowWhenRegisteringExistingUser() {
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest("admin", "admin@gmail.com", "admin2003");

        when(userRepository.existsByEmail("admin@gmail.com")).thenReturn(true);

        CustomBadRequestException exception = assertThrows(
                CustomBadRequestException.class,
                () -> authService.register(request, response)
        );

        assertEquals("User already exist", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login authenticates valid credentials, generates tokens, and sets cookie")
    void shouldLoginSuccessfully() {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest();
        request.setEmail("admin@gmail.com");
        request.setPassword("admin2003");

        when(userRepository.findByEmail("admin@gmail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("admin2003", user.getPassword())).thenReturn(true);
        when(jwtUtil.generateAccessToken(user)).thenReturn("mock-access-token");
        when(jwtUtil.generateRefreshToken(user)).thenReturn("mock-refresh-token");

        AuthDto.AuthResponse authResponse = authService.login(request, response);

        assertNotNull(authResponse);
        assertEquals("mock-access-token", authResponse.accessToken());
        verify(tokenService).saveToken(user, "mock-refresh-token");
        verify(cookieUtil).addCookie("mock-refresh-token", response);
    }

    @Test
    @DisplayName("login throws CustomNotFoundException when user is not found")
    void shouldThrowWhenUserNotFoundOnLogin() {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest();
        request.setEmail("unknown@gmail.com");
        request.setPassword("admin2003");

        when(userRepository.findByEmail("unknown@gmail.com")).thenReturn(Optional.empty());

        CustomNotFoundException exception = assertThrows(
                CustomNotFoundException.class,
                () -> authService.login(request, response)
        );

        assertEquals("User not found", exception.getMessage());
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    @DisplayName("login throws CustomBadRequestException when password does not match")
    void shouldThrowWhenPasswordMismatchOnLogin() {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest();
        request.setEmail("admin@gmail.com");
        request.setPassword("wrong-password");

        when(userRepository.findByEmail("admin@gmail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", user.getPassword())).thenReturn(false);

        CustomBadRequestException exception = assertThrows(
                CustomBadRequestException.class,
                () -> authService.login(request, response)
        );

        assertEquals("Password mismatch", exception.getMessage());
        verify(tokenService, never()).saveToken(any(), any());
    }

    @Test
    @DisplayName("logout removes token from DB and clears cookie")
    void shouldLogoutSuccessfully() {
        String refreshToken = "valid-refresh-token";

        when(jwtUtil.getSubject(refreshToken)).thenReturn("admin@gmail.com");
        when(jwtUtil.validateToken(refreshToken)).thenReturn(true);
        when(userRepository.findByEmail("admin@gmail.com")).thenReturn(Optional.of(user));

        authService.logout(refreshToken, response);

        verify(tokenService).deleteToken(user);
        verify(cookieUtil).clearCookie(response);
    }

    @Test
    @DisplayName("logout throws CustomUnauthorizedException when token is null or empty")
    void shouldThrowWhenLogoutWithNullOrEmptyToken() {
        CustomUnauthorizedException ex1 = assertThrows(
                CustomUnauthorizedException.class,
                () -> authService.logout(null, response)
        );
        assertEquals("Token is null or empty", ex1.getMessage());

        CustomUnauthorizedException ex2 = assertThrows(
                CustomUnauthorizedException.class,
                () -> authService.logout("", response)
        );
        assertEquals("Token is null or empty", ex2.getMessage());
    }

    @Test
    @DisplayName("logout throws CustomUnauthorizedException when token is invalid or expired")
    void shouldThrowWhenLogoutWithInvalidToken() {
        String refreshToken = "invalid-token";

        when(jwtUtil.getSubject(refreshToken)).thenReturn("admin@gmail.com");
        when(jwtUtil.validateToken(refreshToken)).thenReturn(false);

        CustomUnauthorizedException exception = assertThrows(
                CustomUnauthorizedException.class,
                () -> authService.logout(refreshToken, response)
        );
        assertEquals("Token invalid or expired", exception.getMessage());
    }

    @Test
    @DisplayName("logout throws CustomUnauthorizedException when subject is null")
    void shouldThrowWhenLogoutWithNullSubject() {
        String refreshToken = "invalid-token";

        when(jwtUtil.getSubject(refreshToken)).thenReturn(null);

        CustomUnauthorizedException exception = assertThrows(
                CustomUnauthorizedException.class,
                () -> authService.logout(refreshToken, response)
        );
        assertEquals("Token invalid or expired", exception.getMessage());
    }

    @Test
    @DisplayName("refresh successfully re-issues tokens and updates DB and cookie")
    void shouldRefreshSuccessfully() {
        String refreshToken = "valid-refresh-token";
        TokenEntity storedToken = TokenEntity.builder()
                .id(1L)
                .user(user)
                .refreshToken(refreshToken)
                .build();

        when(jwtUtil.validateToken(refreshToken)).thenReturn(true);
        when(jwtUtil.extractClaims(refreshToken)).thenReturn(claims);
        when(claims.get("type", String.class)).thenReturn(JwtUtil.REFRESH);
        when(jwtUtil.getSubject(refreshToken)).thenReturn("admin@gmail.com");
        when(userRepository.findByEmail("admin@gmail.com")).thenReturn(Optional.of(user));
        when(tokenService.getToken(user)).thenReturn(storedToken);

        when(jwtUtil.generateAccessToken(user)).thenReturn("new-access-token");
        when(jwtUtil.generateRefreshToken(user)).thenReturn("new-refresh-token");

        AuthDto.AuthResponse authResponse = authService.refresh(refreshToken, response);

        assertNotNull(authResponse);
        assertEquals("new-access-token", authResponse.accessToken());
        verify(tokenService).saveToken(user, "new-refresh-token");
        verify(cookieUtil).addCookie("new-refresh-token", response);
    }

    @Test
    @DisplayName("refresh throws CustomUnauthorizedException when token is null or blank")
    void shouldThrowWhenRefreshTokenIsMissing() {
        CustomUnauthorizedException ex1 = assertThrows(
                CustomUnauthorizedException.class,
                () -> authService.refresh(null, response)
        );
        assertEquals("Refresh token is missing", ex1.getMessage());

        CustomUnauthorizedException ex2 = assertThrows(
                CustomUnauthorizedException.class,
                () -> authService.refresh("   ", response)
        );
        assertEquals("Refresh token is missing", ex2.getMessage());
    }

    @Test
    @DisplayName("refresh throws CustomUnauthorizedException when token is invalid")
    void shouldThrowWhenRefreshTokenIsInvalid() {
        String refreshToken = "invalid-refresh-token";

        when(jwtUtil.validateToken(refreshToken)).thenReturn(false);

        CustomUnauthorizedException exception = assertThrows(
                CustomUnauthorizedException.class,
                () -> authService.refresh(refreshToken, response)
        );
        assertEquals("Token invalid or expired", exception.getMessage());
    }

    @Test
    @DisplayName("refresh throws CustomUnauthorizedException when token type is not REFRESH")
    void shouldThrowWhenRefreshTokenHasWrongType() {
        String refreshToken = "access-token-passed-as-refresh";

        when(jwtUtil.validateToken(refreshToken)).thenReturn(true);
        when(jwtUtil.extractClaims(refreshToken)).thenReturn(claims);
        when(claims.get("type", String.class)).thenReturn(JwtUtil.ACCESS);

        CustomUnauthorizedException exception = assertThrows(
                CustomUnauthorizedException.class,
                () -> authService.refresh(refreshToken, response)
        );
        assertEquals("Invalid token type", exception.getMessage());
    }

    @Test
    @DisplayName("refresh throws CustomUnauthorizedException when refresh token does not match DB")
    void shouldThrowWhenRefreshTokenDoesNotMatchDB() {
        String refreshToken = "stale-refresh-token";
        TokenEntity storedToken = TokenEntity.builder()
                .id(1L)
                .user(user)
                .refreshToken("another-current-refresh-token")
                .build();

        when(jwtUtil.validateToken(refreshToken)).thenReturn(true);
        when(jwtUtil.extractClaims(refreshToken)).thenReturn(claims);
        when(claims.get("type", String.class)).thenReturn(JwtUtil.REFRESH);
        when(jwtUtil.getSubject(refreshToken)).thenReturn("admin@gmail.com");
        when(userRepository.findByEmail("admin@gmail.com")).thenReturn(Optional.of(user));
        when(tokenService.getToken(user)).thenReturn(storedToken);

        CustomUnauthorizedException exception = assertThrows(
                CustomUnauthorizedException.class,
                () -> authService.refresh(refreshToken, response)
        );
        assertEquals("Refresh token does not match", exception.getMessage());
    }

    @Test
    @DisplayName("findUser throws CustomNotFoundException when user does not exist")
    void testFindUserNotFound() {
        when(userRepository.findByEmail("unknown@gmail.com")).thenReturn(Optional.empty());

        CustomNotFoundException exception = assertThrows(
                CustomNotFoundException.class,
                () -> authService.findUser("unknown@gmail.com")
        );
        assertEquals("User not found", exception.getMessage());
    }
}
