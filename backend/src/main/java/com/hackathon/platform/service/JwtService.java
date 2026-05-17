package com.hackathon.platform.service;

import com.hackathon.platform.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Handles JWT operations: generating tokens, extracting tokens, validation Tokens are made using
 * user ID, role, email, created at time, expiery time.
 */
@Service
public class JwtService {

  @Value("${jwt.secret}")
  private String secret;

  @Value("${jwt.expiration-ms}")
  private long expirationMs;

  /** Makes the signing key */
  private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  /** Generates JWT token. */
  public String generateToken(User user) {
    return Jwts.builder()
        .subject(user.getUserId().toString())
        .claim("email", user.getEmail())
        .claim("role", user.getRole().getName())
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + expirationMs))
        .signWith(getSigningKey())
        .compact();
  }

  /** Extracts claim from a token */
  private Claims extractAllClaims(String token) {
    return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
  }

  /** Extracts user ID from a toke. */
  public UUID extractUserId(String token) {
    return UUID.fromString(extractAllClaims(token).getSubject());
  }

  /**
   * Extracts email from a token.
   *
   * @param token
   * @return
   */
  public String extractEmail(String token) {
    return extractAllClaims(token).get("email", String.class);
  }

  /**
   * Extract claim from token.
   *
   * @param token
   * @return
   */
  public String extractRole(String token) {
    return extractAllClaims(token).get("role", String.class);
  }

  /**
   * cheks if its the right signiture and if its valid.
   *
   * @param token
   * @return
   */
  public boolean isTokenValid(String token) {
    try {
      extractAllClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }
}
