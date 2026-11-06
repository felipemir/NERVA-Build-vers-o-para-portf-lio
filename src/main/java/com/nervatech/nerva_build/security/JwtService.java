package com.nervatech.nerva_build.security;

import com.nervatech.nerva_build.domain.model.AppUser;
import com.nervatech.nerva_build.domain.model.CustomerAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    public static final String PRINCIPAL_TYPE_APP_USER = "APP_USER";
    public static final String PRINCIPAL_TYPE_CUSTOMER = "CUSTOMER";

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(AppUser user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("principalType", PRINCIPAL_TYPE_APP_USER)
                .claim("email", user.getEmail())
                .claim("storeId", user.getStoreId())
                .claim("role", user.getRole())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public String generateCustomerToken(CustomerAccount customerAccount) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(String.valueOf(customerAccount.getId()))
                .claim("principalType", PRINCIPAL_TYPE_CUSTOMER)
                .claim("email", customerAccount.getEmail())
                .claim("role", "CUSTOMER")
                .claim("personType", customerAccount.getPersonType())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long extractUserId(String token) {
        return Long.valueOf(extractAllClaims(token).getSubject());
    }

    public String extractPrincipalType(String token) {
        Object claim = extractAllClaims(token).get("principalType");
        return claim == null ? PRINCIPAL_TYPE_APP_USER : claim.toString();
    }

    public boolean isTokenValid(String token) {
        Date expiration = extractAllClaims(token).getExpiration();
        return expiration != null && expiration.after(new Date());
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
