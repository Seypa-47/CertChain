package com.certchain.auth;

import java.time.Duration;
import java.util.Base64;
import java.util.Set;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AuthSettings {
    private final String issuer;
    private final Duration ttl;
    private final SecretKey key;
    private final String cookieName;
    private final boolean secure;
    private final String sameSite;
    private final String domain;

    public AuthSettings(
        @Value("${app.auth.issuer}") String issuer,
        @Value("${app.auth.access-token-ttl}") Duration ttl,
        @Value("${app.auth.secret-base64}") String secret,
        @Value("${app.auth.cookie-name}") String cookieName,
        @Value("${app.auth.cookie-secure}") boolean secure,
        @Value("${app.auth.cookie-same-site}") String sameSite,
        @Value("${app.auth.cookie-domain}") String domain
    ) {
        if (issuer.isBlank() || ttl.isNegative() || ttl.isZero() || ttl.compareTo(Duration.ofHours(1)) > 0) {
            throw new IllegalArgumentException("Invalid JWT issuer or lifetime");
        }
        byte[] bytes;
        try { bytes = Base64.getDecoder().decode(secret); }
        catch (IllegalArgumentException invalid) { throw new IllegalArgumentException("JWT_SECRET_BASE64 must be base64", invalid); }
        if (bytes.length < 32) throw new IllegalArgumentException("JWT_SECRET_BASE64 must contain at least 32 bytes");
        if (!Set.of("Strict", "Lax", "None").contains(sameSite)) throw new IllegalArgumentException("Invalid SameSite value");
        if ("None".equals(sameSite) && !secure) throw new IllegalArgumentException("SameSite=None requires Secure cookies");
        this.issuer = issuer;
        this.ttl = ttl;
        this.key = new SecretKeySpec(bytes, "HmacSHA256");
        this.cookieName = cookieName;
        this.secure = secure;
        this.sameSite = sameSite;
        this.domain = domain;
    }

    public String issuer() { return issuer; }
    public Duration ttl() { return ttl; }
    public SecretKey key() { return key; }
    public String cookieName() { return cookieName; }
    public boolean secure() { return secure; }
    public String sameSite() { return sameSite; }
    public String domain() { return domain; }
}
