package com.certchain.certificate.dto;

import com.certchain.certificate.Certificate;
import com.certchain.certificate.CertificateLifecycle;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CertificateListItem(UUID id, String certificateId, String recipientName,
                                  String programName, LocalDate issueDate, LocalDate expiryDate,
                                  CertificateLifecycle lifecycle, Instant createdAt) {
    public static CertificateListItem from(Certificate certificate) {
        return new CertificateListItem(certificate.getId(), certificate.getCertificateId(),
            certificate.getRecipientName(), certificate.getProgramName(), certificate.getIssueDate(),
            certificate.getExpiryDate(), certificate.getLifecycle(), certificate.getCreatedAt());
    }
}
