package com.example.financial.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expirationMs}")
    private int jwtExpirationMs;

    @Value("${jwt.cookieName}")
    private String jwtCookie;

    // ─── Cookie Helpers ────────────────────────────────────────────────────────

    public String getJwtFromCookies(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, jwtCookie);
        return (cookie != null) ? cookie.getValue() : null;
    }

    public ResponseCookie generateJwtCookie(UserDetailsImpl userPrincipal) {
        String jwt = generateTokenFromEmail(userPrincipal.getEmail());
        return ResponseCookie.from(jwtCookie, jwt)
                .path("/")
                .maxAge(24 * 60 * 60)
                .httpOnly(true)
                .secure(false) // Set true for HTTPS in production
                .build();
    }

    public ResponseCookie getCleanJwtCookie() {
        return ResponseCookie.from(jwtCookie, "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .build();
    }

    // ─── Token Parsing ─────────────────────────────────────────────────────────

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token) // parseClaimsJws validates signature + expiry
                .getBody()
                .getSubject();
    }

    // ─── Token Validation ──────────────────────────────────────────────────────

    /**
     * Validates a JWT string.
     * <p>
     * Catches every known JJWT exception and always returns {@code false} on
     * failure — never throws, never causes a 500.
     */
    public boolean validateJwtToken(String authToken) {
        if (authToken == null || authToken.isBlank()) {
            logger.debug("JWT validation skipped: token is null or blank");
            return false;
        }
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key())
                    .build()
                    .parseClaimsJws(authToken); // full parse: checks signature + expiry
            return true;

        } catch (SignatureException e) {
            logger.warn("JWT validation failed – invalid signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.warn("JWT validation failed – malformed token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.warn("JWT validation failed – token expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.warn("JWT validation failed – unsupported token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.warn("JWT validation failed – empty/null claims: {}", e.getMessage());
        } catch (Exception e) {
            // Catch-all: never let an unexpected exception propagate out of this method
            logger.error("JWT validation failed – unexpected error: {}", e.getMessage());
        }

        return false; // always false on any exception
    }

    // ─── Token Generation ──────────────────────────────────────────────────────

    public String generateTokenFromEmail(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ─── Key ───────────────────────────────────────────────────────────────────

    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }
}
