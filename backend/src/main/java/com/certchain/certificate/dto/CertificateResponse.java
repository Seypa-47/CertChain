package com.certchain.certificate.dto;

import com.certchain.certificate.CertificateLifecycle;
import com.certchain.certificate.CertificateStatus;
import com.certchain.organization.dto.OrganizationSummary;
import com.certchain.transaction.dto.BlockchainTransactionDto;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CertificateResponse(
    UUID id, String certificateId, OrganizationSummary organization,
    String recipientName, String recipientEmail, String programName, String description,
    LocalDate issueDate, LocalDate expiryDate, CertificateLifecycle lifecycle, CertificateStatus publicStatus,
    String certificateHash, Instant issuedAt, Instant revokedAt,
    Instant createdAt, Instant updatedAt, List<BlockchainTransactionDto> transactions
) {}
