package com.post.server.service;

import com.post.server.entity.TokenEntity;
import com.post.server.entity.UserEntity;
import com.post.server.entity.enums.Role;
import com.post.server.exception.CustomNotFoundException;
import com.post.server.repository.TokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private TokenRepository tokenRepository;

    @InjectMocks
    private TokenService tokenService;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = UserEntity.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encoded_pass")
                .role(Role.USER)
                .build();
    }

    @Test
    @DisplayName("saveToken should create new token when user does not have one")
    void testSaveTokenNew() {
        when(tokenRepository.findByUser(user)).thenReturn(Optional.empty());

        tokenService.saveToken(user, "new-refresh-token");

        ArgumentCaptor<TokenEntity> captor = ArgumentCaptor.forClass(TokenEntity.class);
        verify(tokenRepository).save(captor.capture());

        TokenEntity saved = captor.getValue();
        assertEquals(user, saved.getUser());
        assertEquals("new-refresh-token", saved.getRefreshToken());
        assertNotNull(saved.getExpiration());
        assertTrue(saved.getExpiration().isAfter(LocalDateTime.now()));
    }

    @Test
    @DisplayName("saveToken should update existing token when user already has one")
    void testSaveTokenUpdate() {
        TokenEntity existing = TokenEntity.builder()
                .id(10L)
                .user(user)
                .refreshToken("old-token")
                .expiration(LocalDateTime.now().minusDays(1))
                .build();

        when(tokenRepository.findByUser(user)).thenReturn(Optional.of(existing));

        tokenService.saveToken(user, "updated-token");

        verify(tokenRepository).save(existing);
        assertEquals("updated-token", existing.getRefreshToken());
        assertTrue(existing.getExpiration().isAfter(LocalDateTime.now()));
    }

    @Test
    @DisplayName("getToken returns token when present")
    void testGetTokenSuccess() {
        TokenEntity token = TokenEntity.builder()
                .id(1L)
                .user(user)
                .refreshToken("some-token")
                .build();

        when(tokenRepository.findByUser(user)).thenReturn(Optional.of(token));

        TokenEntity result = tokenService.getToken(user);

        assertNotNull(result);
        assertEquals("some-token", result.getRefreshToken());
    }

    @Test
    @DisplayName("getToken throws NoSuchElementException when not found")
    void testGetTokenNotFound() {
        when(tokenRepository.findByUser(user)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> tokenService.getToken(user));
    }

    @Test
    @DisplayName("deleteToken deletes token when present")
    void testDeleteTokenSuccess() {
        TokenEntity token = TokenEntity.builder()
                .id(1L)
                .user(user)
                .refreshToken("some-token")
                .build();

        when(tokenRepository.findByUser(user)).thenReturn(Optional.of(token));

        tokenService.deleteToken(user);

        verify(tokenRepository).delete(token);
    }

    @Test
    @DisplayName("deleteToken throws exception when token not found")
    void testDeleteTokenNotFound() {
        when(tokenRepository.findByUser(user)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> tokenService.deleteToken(user));
        verify(tokenRepository, never()).delete(any());
    }

    @Test
    @DisplayName("getTokenFromDB returns token when present")
    void testGetTokenFromDBSuccess() {
        TokenEntity token = TokenEntity.builder()
                .id(1L)
                .user(user)
                .refreshToken("token-xyz")
                .build();

        when(tokenRepository.findByUser(user)).thenReturn(Optional.of(token));

        TokenEntity result = tokenService.getTokenFromDB(user);

        assertNotNull(result);
        assertEquals("token-xyz", result.getRefreshToken());
    }

    @Test
    @DisplayName("getTokenFromDB throws CustomNotFoundException when token not found")
    void testGetTokenFromDBNotFound() {
        when(tokenRepository.findByUser(user)).thenReturn(Optional.empty());

        CustomNotFoundException exception = assertThrows(
                CustomNotFoundException.class,
                () -> tokenService.getTokenFromDB(user)
        );

        assertEquals("Token not found", exception.getMessage());
    }
}
