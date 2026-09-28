package com.certchain.auth;

import com.certchain.user.AppUser;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {
    private final AuthSettings settings;
    private final Clock clock;
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;

    public JwtTokenService(AuthSettings settings, Clock clock) {
        this.settings = settings;
        this.clock = clock;
        this.encoder = NimbusJwtEncoder.withSecretKey(settings.key()).algorithm(MacAlgorithm.HS256).build();
        NimbusJwtDecoder configured = NimbusJwtDecoder.withSecretKey(settings.key())
            .macAlgorithm(MacAlgorithm.HS256).build();
        configured.setJwtValidator(JwtValidators.createDefaultWithIssuer(settings.issuer()));
        this.decoder = configured;
    }

    public String issue(AppUser user) {
        Instant now = clock.instant();
        String userId = user.getId().toString();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(settings.issuer())
            .subject(userId)
            .issuedAt(now)
            .expiresAt(now.plus(settings.ttl()))
            .claim("user_id", userId)
            .claim("organization_id", user.getOrganization().getId().toString())
            .claim("role", user.getRole().name())
            .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();
    }

    public Jwt verify(String value) { return decoder.decode(value); }
}
