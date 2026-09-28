package com.certchain.dashboard;

import com.certchain.certificate.CertificateLifecycle;
import com.certchain.certificate.CertificateStatus;
import com.certchain.transaction.BlockchainTransactionStatus;
import com.certchain.transaction.BlockchainTransactionType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DashboardResponse(long totalIssued, long valid, long expired, long revoked,
    List<RecentCertificate> recentCertificates, List<RecentTransaction> recentTransactions) {
    public record RecentCertificate(UUID id, String certificateId, String recipientName, String programName,
        CertificateLifecycle lifecycle, CertificateStatus publicStatus, LocalDate issueDate, Instant createdAt) {}
    public record RecentTransaction(UUID certificateId, String publicCertificateId,
        BlockchainTransactionType type, BlockchainTransactionStatus status, String transactionHash,
        String network, Instant createdAt, Instant confirmedAt) {}
}
