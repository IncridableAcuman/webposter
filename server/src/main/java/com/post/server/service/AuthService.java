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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;
    private final PasswordEncoder passwordEncoder;

    public UserEntity findUser(String email){
        return userRepository.findByEmail(email).orElseThrow(()-> new CustomNotFoundException("User not found"));
    }
    public AuthDto.AuthResponse authResponse(UserEntity user,HttpServletResponse response){
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);
        tokenService.saveToken(user,refreshToken);
        cookieUtil.addCookie(refreshToken,response);
        return AuthDto.AuthResponse.from(accessToken);
    }

    @Transactional
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest request, HttpServletResponse response){
        if (userRepository.existsByEmail(request.getEmail())){
            throw new CustomBadRequestException("User already exist");}
        UserEntity user = UserEntity
                .builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();
        userRepository.save(user);
        return authResponse(user,response);
    }
    public AuthDto.AuthResponse login(AuthDto.LoginRequest request,HttpServletResponse response){
        UserEntity user = findUser(request.getEmail());
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())){
            throw new CustomBadRequestException("Password mismatch");}
        return authResponse(user,response);
    }
    public void logout(String refreshToken,HttpServletResponse response){
        if (refreshToken == null || refreshToken.isEmpty()){
            throw new CustomUnauthorizedException("Token is null or empty");}
        String email = jwtUtil.getSubject(refreshToken);
        if (email == null || !jwtUtil.validateToken(refreshToken)){
            throw new CustomUnauthorizedException("Token invalid or expired");}
        UserEntity user = findUser(email);
        tokenService.deleteToken(user);
        cookieUtil.clearCookie(response);
    }
    public AuthDto.AuthResponse refresh(String refreshToken,HttpServletResponse response){
        if (refreshToken == null || refreshToken.isBlank()){
            throw new CustomUnauthorizedException("Refresh token is missing");}
        if (!jwtUtil.validateToken(refreshToken)){
            throw new CustomUnauthorizedException("Token invalid or expired");}
        Claims claims = jwtUtil.extractClaims(refreshToken);
        String type = claims.get("type",String.class);
        if (!JwtUtil.REFRESH.equals(type)){
            throw new CustomUnauthorizedException("Invalid token type");
        }
        String email = jwtUtil.getSubject(refreshToken);
        UserEntity user = findUser(email);
        TokenEntity token;
        try {
            token = tokenService.getToken(user);
        } catch (Exception e) {
            throw new CustomUnauthorizedException("Refresh token does not match");
        }
        if (!token.getRefreshToken().equals(refreshToken)){
            throw new CustomUnauthorizedException("Refresh token does not match");
        }
        return authResponse(user,response);
    }
}
