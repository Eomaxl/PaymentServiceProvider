package com.paymentprovider.paymentservice.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * Service for JWT token generation and validation.
 */
@Service
public class JwtService {
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey secretKey;
    private final long accessTokenExpirationMinutes;
    private final long refreshTokenExpirationDays;
    private final String issuer;

    public JwtService(
            @Value("${payment.security.jwt.secret}") String secret,
            @Value("${payment.security.jwt.access-token-expiration-minutes:15}") long accessTokenExpirationMinutes,
            @Value("${payment.security.jwt.refresh-token-expiration-days:7}") long refreshTokenExpirationDays,
            @Value("${payment.security.jwt.issuer:payment-service}") String issuer) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenExpirationMinutes = accessTokenExpirationMinutes;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
        this.issuer = issuer;
    }

    /**
     * Generates an access token for the authenticated user.
     */
    public String generateAccessToken(AuthenticatedUser user) {
        Instant now = Instant.now();
        Instant expiration = now.plus(accessTokenExpirationMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(user.getUserId())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .claim("username", user.getUsername())
                .claim("merchantId", user.getMerchantId())
                .claim("roles", user.getRoles())
                .claim("authType", user.getAuthenticationType().name())
                .signWith(secretKey)
                .compact();
    }

    /**
     * Generates a refresh token for the authenticated user.
     */
    public String generateRefreshToken(AuthenticatedUser user) {
        Instant now = Instant.now();
        Instant expiration = now.plus(refreshTokenExpirationDays, ChronoUnit.DAYS);

        return Jwts.builder()
                .subject(user.getUserId())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .claim("tokenType", "refresh")
                .signWith(secretKey)
                .compact();
    }

    /**
     * Validates and parses a JWT token.
     */
    public Claims validateToken(String token) throws JwtException {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            logger.debug("JWT token expired: {}", e.getMessage());
            throw new JwtException("Token expired", e);
        } catch (UnsupportedJwtException e) {
            logger.warn("Unsupported JWT token: {}", e.getMessage());
            throw new JwtException("Unsupported token", e);
        } catch (MalformedJwtException e) {
            logger.warn("Malformed JWT token: {}", e.getMessage());
            throw new JwtException("Malformed token", e);
        } catch (SecurityException e) {
            logger.warn("Invalid JWT signature: {}", e.getMessage());
            throw new JwtException("Invalid signature", e);
        } catch (IllegalArgumentException e) {
            logger.warn("JWT token compact of handler are invalid: {}", e.getMessage());
            throw new JwtException("Invalid token", e);
        }
    }

    /**
     * Extracts user information from JWT claims.
     */
    @SuppressWarnings("unchecked")
    public AuthenticatedUser extractUser(Claims claims) {
        String userId = claims.getSubject();
        String username = claims.get("username", String.class);
        String merchantId = claims.get("merchantId", String.class);

        // Extract roles
        Object rolesObj = claims.get("roles");
        Set<UserRole> roles = Set.of();
        if (rolesObj instanceof Iterable) {
            roles = ((Iterable<String>) rolesObj).iterator().hasNext()
                    ? Set.of(UserRole.valueOf(((Iterable<String>) rolesObj).iterator().next()))
                    : Set.of();
        }

        String authTypeStr = claims.get("authType", String.class);
        AuthenticationType authType = authTypeStr != null
                ? AuthenticationType.valueOf(authTypeStr)
                : AuthenticationType.JWT;

        return new AuthenticatedUser(userId, username, merchantId, roles, authType);
    }

    /**
     * Checks if a token is expired.
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = validateToken(token);
            return claims.getExpiration().before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }
}