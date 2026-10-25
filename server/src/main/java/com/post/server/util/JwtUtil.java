package com.post.server.util;

import com.post.server.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.refresh_time}")
    private long refreshTime;
    @Value("${jwt.access_time}")
    private long accessTime;
    private SecretKey secretKey;

    public static final String ACCESS="ACCESS";
    public static final String REFRESH="REFRESH";

    @PostConstruct
    public void init(){
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    private String createToken(UserEntity user, long tokenExpiration,String tokenType){
        long currentMillis = System.currentTimeMillis();
        Date issueAt = new Date(currentMillis);
        Date expiration = new Date(currentMillis + tokenExpiration);
        Map<String,Object> claims = new HashMap<>();
        claims.put("id",user.getId());
        claims.put("role",user.getRole());
        claims.put("type",tokenType);
        return Jwts
                .builder()
                .subject(user.getEmail())
                .claims(claims)
                .issuedAt(issueAt)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }
    public String generateRefreshToken(UserEntity user){
        return createToken(user,refreshTime,"REFRESH");
    }
    public String generateAccessToken(UserEntity user){
        return createToken(user,accessTime,"ACCESS");
    }
    public Claims extractClaims(String token){
        return Jwts
                .parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();


    }
    public String getSubject(String token){
        return extractClaims(token).getSubject();
    }
    public Date getExpiration(String token){
        return extractClaims(token).getExpiration();
    }
    public boolean isTokenExpired(String token){
        return getExpiration(token).before(new Date());
    }
    public boolean validateToken(String token){
        try {
            return !isTokenExpired(token) && getSubject(token) != null;
        } catch (JwtException | IllegalArgumentException e){
            return false;
        }
    }
}
