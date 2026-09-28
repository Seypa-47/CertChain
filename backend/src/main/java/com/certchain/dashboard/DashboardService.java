package com.certchain.dashboard;

import com.certchain.certificate.CertificateLifecycle;
import com.certchain.certificate.CertificateRepository;
import com.certchain.certificate.CertificateStatusService;
import com.certchain.transaction.BlockchainTransactionRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private final CertificateRepository certificates;
    private final BlockchainTransactionRepository transactions;
    private final CertificateStatusService statuses;
    private final Clock clock;

    public DashboardService(CertificateRepository certificates, BlockchainTransactionRepository transactions,
        CertificateStatusService statuses, Clock clock) {
        this.certificates = certificates;
        this.transactions = transactions;
        this.statuses = statuses;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardResponse get(UUID organizationId) {
        LocalDate today = LocalDate.now(clock);
        CertificateLifecycle issued = CertificateLifecycle.ISSUED;
        var recentCertificates = certificates.findRecentForOrganization(organizationId, PageRequest.of(0, 5))
            .stream().map(c -> new DashboardResponse.RecentCertificate(c.getId(), c.getCertificateId(),
                c.getRecipientName(), c.getProgramName(), c.getLifecycle(),
                c.getLifecycle() == issued ? statuses.status(c) : null, c.getIssueDate(), c.getCreatedAt())).toList();
        var recentTransactions = transactions.findRecentForOrganization(organizationId, PageRequest.of(0, 5))
            .stream().map(t -> new DashboardResponse.RecentTransaction(t.getCertificate().getId(),
                t.getCertificate().getCertificateId(), t.getTransactionType(), t.getStatus(),
                t.getTransactionHash(), t.getNetwork(), t.getCreatedAt(), t.getConfirmedAt())).toList();
        return new DashboardResponse(certificates.countIssued(organizationId, issued),
            certificates.countValid(organizationId, issued, today),
            certificates.countExpired(organizationId, issued, today),
            certificates.countRevoked(organizationId, issued), recentCertificates, recentTransactions);
    }
}
