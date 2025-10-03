package com.tanit.cto.user_management.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;

// Generate and validate JWT tokens
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private final long expiration = 1000 * 60 * 60 * 24; // 24 hours

    // Generates a JWT token for the given email
    public String generateToken(String email, long customExpiration) {
        return generateToken(email, Set.of("USER"), customExpiration);
    }

    public String generateToken(String email, Set<String> roles, long customExpiration) {
        String token = Jwts.builder()
                .setSubject(email)
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + customExpiration))
                .signWith(SignatureAlgorithm.HS512, secret.getBytes(StandardCharsets.UTF_8))
                .compact();
        return token;
    }

    public String generateToken(String email) {
        return generateToken(email, Set.of("USER"), expiration); // use default 24h
    }

    // Extracts the email from the token
    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    // Checks if the token is valid
    public boolean validateToken(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    // Extracts claims from the token
    private Claims getClaims(String token) {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }
}
