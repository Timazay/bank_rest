package com.example.bankcards.util;

import com.example.bankcards.entity.RefreshToken;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.TokenType;
import com.example.bankcards.exception.AuthException;
import com.example.bankcards.repository.RefreshTokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtUtils {

    @Value("${jwt.access.secret}")
    private String accessSecret;

    @Value("${jwt.refresh.secret}")
    private String refreshSecret;

    @Value("${jwt.access.expiration}")
    private Long accessExpiration;

    @Value("${jwt.refresh.expiration}")
    private Long refreshExpiration;

    private final RefreshTokenRepository refreshTokenRepository;

    public String generateAccessToken(User user) {
        return generateToken(user, accessExpiration, getSigningKey(accessSecret));
    }

    public RefreshToken generateRefreshToken(User user) {
        String token = generateToken(user, refreshExpiration, getSigningKey(refreshSecret));

        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .user(user)
                .expiryDate(Instant.now().plusMillis(refreshExpiration))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public Date extractExpiration(String token, TokenType type) {
        Claims claims = extractAllClaims(token, type);
        return claims.getExpiration();
    }

    public String extractEmail(String token, TokenType type) {
        Claims claims = extractAllClaims(token, type);
        return claims.getSubject();
    }

    private String generateToken(User user, Long expiration, Key key) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("roles", user.getRoles().stream().map(Role::getName).toList())
                .claim("userId", user.getId())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token, TokenType type) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSecret(type))
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims extractAllClaims(String token, TokenType type) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey(getSecret(type)))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSigningKey(String secret) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public RefreshToken verifyExistingRefreshToken(String token, TokenType type) {

        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new AuthException("Refresh token not found"));

        if (!validateToken(token, type)) {
            refreshTokenRepository.delete(refreshToken);
            throw new AuthException("Update refresh token");
        }

        if (refreshToken.getUser() == null)
            throw new AuthException("User not found");

        return refreshToken;
    }

    private String getSecret(TokenType type) {
        return switch (type) {
            case ACCESS -> accessSecret;
            case REFRESH -> refreshSecret;
        };
    }
}
