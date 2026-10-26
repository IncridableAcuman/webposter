package com.post.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.post.server.dto.AuthDto;
import com.post.server.exception.CustomBadRequestException;
import com.post.server.exception.CustomNotFoundException;
import com.post.server.exception.CustomUnauthorizedException;
import com.post.server.exception.GlobalExceptionHandler;
import com.post.server.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /auth/register returns 200 with access token on valid input")
    void register_success() throws Exception {
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest("john_doe", "john@example.com", "password123");
        AuthDto.AuthResponse authResponse = new AuthDto.AuthResponse("access-token-123");

        when(authService.register(eq(request), any(HttpServletResponse.class))).thenReturn(authResponse);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"));

        verify(authService).register(eq(request), any(HttpServletResponse.class));
    }

    @Test
    @DisplayName("POST /auth/register returns 400 when user already exists")
    void register_userAlreadyExists() throws Exception {
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest("john_doe", "john@example.com", "password123");

        when(authService.register(eq(request), any(HttpServletResponse.class)))
                .thenThrow(new CustomBadRequestException("User already exist"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User already exist"));
    }

    @Test
    @DisplayName("POST /auth/login returns 200 with access token on valid credentials")
    void login_success() throws Exception {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("password123");
        AuthDto.AuthResponse authResponse = new AuthDto.AuthResponse("access-token-123");

        when(authService.login(eq(request), any(HttpServletResponse.class))).thenReturn(authResponse);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"));

        verify(authService).login(eq(request), any(HttpServletResponse.class));
    }

    @Test
    @DisplayName("POST /auth/login returns 404 when user is not found")
    void login_userNotFound() throws Exception {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("password123");

        when(authService.login(eq(request), any(HttpServletResponse.class)))
                .thenThrow(new CustomNotFoundException("User not found"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    @DisplayName("POST /auth/login returns 400 when password mismatches")
    void login_passwordMismatch() throws Exception {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("wrongpassword");

        when(authService.login(eq(request), any(HttpServletResponse.class)))
                .thenThrow(new CustomBadRequestException("Password mismatch"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password mismatch"));
    }

    @Test
    @DisplayName("POST /auth/logout returns 200 with Logged out message")
    void logout_success() throws Exception {
        doNothing().when(authService).logout(eq("refresh-token-cookie"), any(HttpServletResponse.class));

        mockMvc.perform(post("/auth/logout")
                        .cookie(new Cookie("refreshToken", "refresh-token-cookie")))
                .andExpect(status().isOk())
                .andExpect(content().string("Logged out"));

        verify(authService).logout(eq("refresh-token-cookie"), any(HttpServletResponse.class));
    }

    @Test
    @DisplayName("POST /auth/logout returns 401 when token is invalid or missing")
    void logout_unauthorized() throws Exception {
        doThrow(new CustomUnauthorizedException("Token is null or empty"))
                .when(authService).logout(isNull(), any(HttpServletResponse.class));

        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token is null or empty"));
    }

    @Test
    @DisplayName("POST /auth/refresh returns 200 with new access token")
    void refresh_success() throws Exception {
        AuthDto.AuthResponse authResponse = new AuthDto.AuthResponse("new-access-token");

        when(authService.refresh(eq("refresh-token-cookie"), any(HttpServletResponse.class)))
                .thenReturn(authResponse);

        mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("refreshToken", "refresh-token-cookie")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"));

        verify(authService).refresh(eq("refresh-token-cookie"), any(HttpServletResponse.class));
    }

    @Test
    @DisplayName("POST /auth/refresh returns 401 when refresh token is missing or expired")
    void refresh_unauthorized() throws Exception {
        when(authService.refresh(isNull(), any(HttpServletResponse.class)))
                .thenThrow(new CustomUnauthorizedException("Refresh token is missing"));

        mockMvc.perform(post("/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token is missing"));
    }
}
