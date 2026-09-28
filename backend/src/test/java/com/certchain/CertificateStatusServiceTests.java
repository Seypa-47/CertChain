package com.certchain;

import com.certchain.certificate.Certificate;
import com.certchain.certificate.CertificateStatus;
import com.certchain.certificate.CertificateStatusService;
import com.certchain.organization.Organization;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CertificateStatusServiceTests {
    @Test void expiryChangesAtUtcDateBoundaryAndRevocationAlwaysWins() {
        Certificate certificate = new Certificate("CERT-2026-000001",
            new Organization("Org", "org@example.com"), "Recipient", "recipient@example.com",
            "Course", LocalDate.of(2026, 9, 1));
        certificate.setExpiryDate(LocalDate.of(2026, 9, 28));
        assertEquals(CertificateStatus.VALID, statusAt(certificate, "2026-09-28T23:59:59Z"));
        assertEquals(CertificateStatus.EXPIRED, statusAt(certificate, "2026-09-29T00:00:00Z"));
        certificate.beginIssuance("a".repeat(64), "v1");
        certificate.markIssued(Instant.parse("2026-09-01T12:00:00Z"));
        certificate.setRevocationReason("Private correction");
        certificate.markRevoked(Instant.parse("2026-09-29T01:00:00Z"));
        assertEquals(CertificateStatus.REVOKED, statusAt(certificate, "2026-09-29T02:00:00Z"));
        assertEquals(CertificateStatus.REVOKED, statusAt(certificate, "2026-10-01T00:00:00Z"));
    }

    private static CertificateStatus statusAt(Certificate certificate, String instant) {
        return new CertificateStatusService(Clock.fixed(Instant.parse(instant), ZoneOffset.UTC)).status(certificate);
    }
}
