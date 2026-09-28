package com.certchain;

import com.certchain.certificate.CertificateHashService;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CertificateHashServiceTests {
    private final CertificateHashService service = new CertificateHashService();
    private static final UUID ORG = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final LocalDate DATE = LocalDate.of(2026, 9, 20);

    private String canonical(String id, String name, String program, UUID org,
                             LocalDate date, LocalDate expiry) {
        return service.canonicalize(id, name, program, org, date, expiry);
    }

    @Test void knownVectorAndDeterminism() {
        String canonical = canonical("CERT-2026-000001", "José", "Blockchain Fundamentals", ORG, DATE, null);
        assertEquals("v1|certificateId=CERT-2026-000001|recipientName=José"
            + "|programName=Blockchain Fundamentals|organizationId=" + ORG
            + "|issueDate=2026-09-20|expiryDate=", canonical);
        assertEquals("ffe6071019492a2d0b5ff0db36648e03f91dc0c80cd7ef28c3c24534b3aaf204",
            service.hash(canonical));
        assertEquals(service.hash(canonical), service.hash(canonical));
    }

    @Test void everyProofFieldChangesTheHash() {
        String original = canonical("CERT-2026-000001", "José", "Course", ORG, DATE, null);
        String baseline = service.hash(original);
        assertNotEquals(baseline, service.hash(canonical("CERT-2026-000002", "José", "Course", ORG, DATE, null)));
        assertNotEquals(baseline, service.hash(canonical("CERT-2026-000001", "Jose", "Course", ORG, DATE, null)));
        assertNotEquals(baseline, service.hash(canonical("CERT-2026-000001", "José", "course", ORG, DATE, null)));
        assertNotEquals(baseline, service.hash(canonical("CERT-2026-000001", "José", "Course",
            UUID.fromString("00000000-0000-0000-0000-000000000002"), DATE, null)));
        assertNotEquals(baseline, service.hash(canonical("CERT-2026-000001", "José", "Course", ORG,
            DATE.plusDays(1), null)));
        assertNotEquals(baseline, service.hash(canonical("CERT-2026-000001", "José", "Course", ORG,
            DATE, DATE.plusYears(1))));
    }

    @Test void normalizesUnicodeWhitespaceAndCertificateId() {
        String expected = canonical("CERT-2026-000001", "José A", "My Course", ORG, DATE, null);
        String variant = canonical("  cert-2026-000001  ", " Jose\u0301\u00a0\tA  ",
            "\nMy\u2003Course\r", ORG, DATE, null);
        assertEquals(expected, variant);
        assertEquals(service.contractKey(" cert-2026-000001 "), service.contractKey("CERT-2026-000001"));
    }

    @Test void escapesReservedCharactersInFixedOrder() {
        String canonical = canonical("cert-2026-000001", "A\\|= B", "X=Y|Z\\Q", ORG, DATE, null);
        assertTrue(canonical.contains("|recipientName=A\\\\\\|\\= B|programName=X\\=Y\\|Z\\\\Q|"));
        assertTrue(canonical.indexOf("|certificateId=") < canonical.indexOf("|recipientName="));
        assertTrue(canonical.indexOf("|recipientName=") < canonical.indexOf("|programName="));
        assertTrue(canonical.indexOf("|programName=") < canonical.indexOf("|organizationId="));
        assertTrue(canonical.endsWith("|expiryDate="));
    }
}
