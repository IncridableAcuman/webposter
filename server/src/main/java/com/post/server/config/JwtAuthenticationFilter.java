package com.post.server.config;

import com.post.server.service.LoadUserDetailsService;
import com.post.server.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.post.server.util.JwtUtil.ACCESS;

@Configuration
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final LoadUserDetailsService loadUserDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String headers = request.getHeader("Authorization");
        if (headers == null || !headers.startsWith("Bearer ")){
            filterChain.doFilter(request,response);
            return;
        }
        String token = headers.substring(7);
        try {
            if (jwtUtil.validateToken(token)){
                Claims claims = jwtUtil.extractClaims(token);
                String tokenType = claims.get("type",String.class);

                if (!ACCESS.equals(tokenType)){
                    filterChain.doFilter(request,response);
                    return;
                }
                String email = claims.getSubject();

                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null){
                    UserDetails userDetails = loadUserDetailsService.loadUserByUsername(email);
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException exception){
            SecurityContextHolder.clearContext();

            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Token invalid or expired\"}");
        }
        filterChain.doFilter(request,response);
    }
}
