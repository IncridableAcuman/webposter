package com.post.server.service;

import com.post.server.repository.UserRepository;
import com.post.server.util.CookieUtil;
import com.post.server.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;
}
