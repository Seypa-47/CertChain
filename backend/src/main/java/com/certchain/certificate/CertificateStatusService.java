package com.certchain.certificate;

import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class CertificateStatusService {
    private final Clock clock;

    public CertificateStatusService(Clock clock) { this.clock = clock; }

    public CertificateStatus status(Certificate certificate) {
        if (certificate.getRevokedAt() != null) return CertificateStatus.REVOKED;
        LocalDate expiry = certificate.getExpiryDate();
        if (expiry != null && expiry.isBefore(LocalDate.now(clock))) return CertificateStatus.EXPIRED;
        return CertificateStatus.VALID;
    }
}
