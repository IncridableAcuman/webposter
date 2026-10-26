package com.post.server.service;

import com.post.server.entity.TokenEntity;
import com.post.server.entity.UserEntity;
import com.post.server.exception.CustomNotFoundException;
import com.post.server.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TokenService {


    private final TokenRepository tokenRepository;

    public void saveToken(UserEntity user,String refreshToken){
        TokenEntity token = tokenRepository.findByUser(user).orElseGet(TokenEntity::new);
        token.setUser(user);
        token.setRefreshToken(refreshToken);
        token.setExpiration(LocalDateTime.now().plusDays(7));
        tokenRepository.save(token);
    }
    public TokenEntity getToken(UserEntity user){
        return tokenRepository.findByUser(user).orElseThrow();
    }
    public void deleteToken(UserEntity user){
        TokenEntity token = getToken(user);
        tokenRepository.delete(token);
    }
    public TokenEntity getTokenFromDB(UserEntity user){
        return tokenRepository.findByUser(user).orElseThrow(()-> new CustomNotFoundException("Token not found"));
    }
}
