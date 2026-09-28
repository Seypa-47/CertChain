package com.certchain;

import com.certchain.certificate.PublicVerificationRateLimiter;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class PublicVerificationRateLimiterTests {
    @Test void limitsRepeatedRequestsWithoutAffectingOtherAddresses() {
        var limiter = new PublicVerificationRateLimiter(
            Clock.fixed(Instant.parse("2026-09-28T00:00:00Z"), ZoneOffset.UTC));
        for (int i = 0; i < 120; i++) limiter.check("192.0.2.1");
        assertEquals(429, assertThrows(ResponseStatusException.class,
            () -> limiter.check("192.0.2.1")).getStatusCode().value());
        assertDoesNotThrow(() -> limiter.check("192.0.2.2"));
    }
}
