package com.sk.mi.util;

import com.sk.mi.entity.master.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.util.Date;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtUtil {
  private final JwtProperties jwtProperties;
  private SecretKey key;

  private SecretKey getKey() {
    if (key == null) {
      key = Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes());
    }
    return key;
  }

  public String generateAccessToken(User user) {
    return Jwts.builder()
        .subject(user.getUsername())
        .claim("tenant", user.getCompany().getName())
        .claim("role", user.getRole().name())
        .claim("permissions", user.getPermissions().stream().map(Enum::name).collect(Collectors.toList()))
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessTokenExpiry()))
        .signWith(getKey())
        .compact();
  }

  public String generateRefreshToken(String username) {
    return Jwts.builder()
        .subject(username)
        .expiration(new Date(System.currentTimeMillis() + jwtProperties.getRefreshTokenExpiry()))
        .signWith(getKey())
        .compact();
  }

  public Claims extractClaims(String token) {
    return Jwts.parser()
        .verifyWith(getKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }
  public String extractUsername(String token) {
    return extractClaims(token).getSubject();
  }

  public boolean isTokenExpired(String token) {
    return extractClaims(token)
        .getExpiration()
        .before(new Date());
  }
}
