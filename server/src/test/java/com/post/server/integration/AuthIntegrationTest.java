package com.post.server.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.post.server.dto.AuthDto;
import com.post.server.entity.TokenEntity;
import com.post.server.entity.UserEntity;
import com.post.server.entity.enums.Role;
import com.post.server.repository.TokenRepository;
import com.post.server.repository.UserRepository;
import com.post.server.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        tokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Full Registration Flow: saves user and token, returns accessToken and cookie")
    void testRegisterFlow() throws Exception {
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest("john_doe", "john@example.com", "password123");

        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        // 1. Verify response cookie
        String cookieHeader = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(cookieHeader);
        assertTrue(cookieHeader.contains("refreshToken="));
        assertTrue(cookieHeader.contains("HttpOnly"));

        // 2. Verify user in database
        Optional<UserEntity> userOpt = userRepository.findByEmail("john@example.com");
        assertTrue(userOpt.isPresent());
        UserEntity user = userOpt.get();
        assertEquals("john_doe", user.getUsername());
        assertEquals(Role.USER, user.getRole());
        assertTrue(passwordEncoder.matches("password123", user.getPassword()));

        // 3. Verify token in database
        Optional<TokenEntity> tokenOpt = tokenRepository.findByUser(user);
        assertTrue(tokenOpt.isPresent());
        assertNotNull(tokenOpt.get().getRefreshToken());

        // 4. Verify accessToken is valid and contains user email
        String responseContent = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseContent);
        String accessToken = jsonNode.get("accessToken").asText();
        assertTrue(jwtUtil.validateToken(accessToken));
        assertEquals("john@example.com", jwtUtil.getSubject(accessToken));
    }

    @Test
    @DisplayName("Registration Rejection: registering duplicate email returns 400")
    void testRegisterDuplicateEmail() throws Exception {
        AuthDto.RegisterRequest firstRequest = new AuthDto.RegisterRequest("john_doe", "john@example.com", "password123");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isOk());

        AuthDto.RegisterRequest duplicateRequest = new AuthDto.RegisterRequest("another_user", "john@example.com", "anotherpass123");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User already exist"));
    }

    @Test
    @DisplayName("Full Login Flow: authenticates user and updates refresh token")
    void testLoginFlow() throws Exception {
        // Register user first
        UserEntity user = UserEntity.builder()
                .username("jane_doe")
                .email("jane@example.com")
                .password(passwordEncoder.encode("secretpass123"))
                .role(Role.USER)
                .build();
        userRepository.save(user);

        AuthDto.LoginRequest loginRequest = new AuthDto.LoginRequest();
        loginRequest.setEmail("jane@example.com");
        loginRequest.setPassword("secretpass123");

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        String cookieHeader = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(cookieHeader);
        assertTrue(cookieHeader.contains("refreshToken="));

        // Verify token saved in DB
        Optional<TokenEntity> tokenOpt = tokenRepository.findByUser(user);
        assertTrue(tokenOpt.isPresent());
    }

    @Test
    @DisplayName("Login Failure: wrong password returns 400 Bad Request")
    void testLoginWrongPassword() throws Exception {
        UserEntity user = UserEntity.builder()
                .username("jane_doe")
                .email("jane@example.com")
                .password(passwordEncoder.encode("secretpass123"))
                .role(Role.USER)
                .build();
        userRepository.save(user);

        AuthDto.LoginRequest loginRequest = new AuthDto.LoginRequest();
        loginRequest.setEmail("jane@example.com");
        loginRequest.setPassword("wrongpassword");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password mismatch"));
    }

    @Test
    @DisplayName("Login Failure: unregistered email returns 404 Not Found")
    void testLoginUnregisteredUser() throws Exception {
        AuthDto.LoginRequest loginRequest = new AuthDto.LoginRequest();
        loginRequest.setEmail("unknown@example.com");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    @DisplayName("Full Token Refresh Flow: refreshes access token and rotates refresh token")
    void testRefreshFlow() throws Exception {
        UserEntity user = UserEntity.builder()
                .username("bob_doe")
                .email("bob@example.com")
                .password(passwordEncoder.encode("secretpass123"))
                .role(Role.USER)
                .build();
        user = userRepository.save(user);

        String initialRefreshToken = jwtUtil.generateRefreshToken(user);
        TokenEntity tokenEntity = TokenEntity.builder()
                .user(user)
                .refreshToken(initialRefreshToken)
                .build();
        tokenRepository.save(tokenEntity);

        MvcResult result = mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("refreshToken", initialRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        // Check new cookie returned
        String newCookieHeader = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(newCookieHeader);
        assertTrue(newCookieHeader.contains("refreshToken="));

        // Check DB token is updated
        Optional<TokenEntity> updatedToken = tokenRepository.findByUser(user);
        assertTrue(updatedToken.isPresent());
        assertNotNull(updatedToken.get().getRefreshToken());
    }

    @Test
    @DisplayName("Refresh Failure: invalid or missing refresh token returns 401")
    void testRefreshWithMissingOrInvalidToken() throws Exception {
        // Missing cookie
        mockMvc.perform(post("/auth/refresh"))
                .andExpect(status().isUnauthorized());

        // Invalid token
        mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("refreshToken", "invalid-token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Full Logout Flow: deletes token from DB and clears cookie")
    void testLogoutFlow() throws Exception {
        UserEntity user = UserEntity.builder()
                .username("alice_doe")
                .email("alice@example.com")
                .password(passwordEncoder.encode("secretpass123"))
                .role(Role.USER)
                .build();
        user = userRepository.save(user);

        String refreshToken = jwtUtil.generateRefreshToken(user);
        TokenEntity tokenEntity = TokenEntity.builder()
                .user(user)
                .refreshToken(refreshToken)
                .build();
        tokenRepository.save(tokenEntity);

        MvcResult result = mockMvc.perform(post("/auth/logout")
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andReturn();

        // 1. Verify cookie is cleared (Max-Age=0)
        String cookieHeader = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(cookieHeader);
        assertTrue(cookieHeader.contains("Max-Age=0"));

        // 2. Verify token is deleted from database
        Optional<TokenEntity> tokenOpt = tokenRepository.findByUser(user);
        assertFalse(tokenOpt.isPresent());

        // 3. Attempting to refresh after logout should fail
        mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Security Protection: unauthenticated request to protected endpoint is rejected")
    void testProtectedEndpointAccess() throws Exception {
        // Protected path should be denied without authentication (returns 401 or 403)
        mockMvc.perform(get("/tasks"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 401 || status == 403,
                            "Expected 401 or 403 but got: " + status);
                });

        // With valid Access Token, user passes security filter
        UserEntity user = UserEntity.builder()
                .username("secure_user")
                .email("secure@example.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.USER)
                .build();
        userRepository.save(user);
        String accessToken = jwtUtil.generateAccessToken(user);

        // Security filter authenticates the user; endpoint returns 200 OK
        mockMvc.perform(get("/tasks")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
