package com.billingapp.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {

    // ⚠️ In production, put this in application.properties
    // Must be at least 32 characters long for HS256 security
    private static final String SECRET = "MySuperSecretKeyForBillingApp1234567890!"; 
    private static final long EXPIRATION_TIME = 86400000; // 24 hours (in milliseconds)

    private final Key key = Keys.hmacShaKeyFor(SECRET.getBytes());

    // 1. Generate Token
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 2. Get Username from Token
    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // 3. Validate Token
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            System.err.println("Invalid JWT Token: " + e.getMessage());
        }
        return false;
    }
}