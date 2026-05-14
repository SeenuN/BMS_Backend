package com.seenu.bankingsystem.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private final String SECRET = "seenuBankingSecretKeyseenuBankingSecretKey";

    private Key getSignKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    public String generateToken(String email, int roleId) {

        return Jwts.builder()
                .setSubject(email)
                .claim("role", roleId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60*60))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    public String extractUsername(String token){

        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token, String username){

        String extracted = extractUsername(token);

        return extracted.equals(username);
    }

}