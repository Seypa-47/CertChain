package com.certchain.certificate;

import com.certchain.blockchain.CertificateRegistryGateway.ChainIdentity;
import com.certchain.blockchain.CertificateRegistryGateway.IssueReceipt;
import com.certchain.transaction.BlockchainTransaction;
import com.certchain.transaction.BlockchainTransactionRepository;
import com.certchain.transaction.BlockchainTransactionStatus;
import com.certchain.transaction.BlockchainTransactionType;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class IssuancePersistence {
    private final CertificateRepository certificates;
    private final BlockchainTransactionRepository transactions;
    private final CertificateHashService hashes;
    private final Clock clock;

    public IssuancePersistence(CertificateRepository certificates,
                               BlockchainTransactionRepository transactions,
                               CertificateHashService hashes, Clock clock) {
        this.certificates = certificates;
        this.transactions = transactions;
        this.hashes = hashes;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Snapshot snapshot(UUID certificateId, UUID organizationId) {
        Certificate certificate = certificates.findByIdAndOrganizationId(certificateId, organizationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificate not found"));
        return snapshot(certificate);
    }

    @Transactional(readOnly = true)
    public UUID organizationId(UUID certificateId) {
        return certificates.findById(certificateId).orElseThrow().getOrganization().getId();
    }

    @Transactional
    public Prepared begin(UUID certificateId, UUID organizationId, ChainIdentity chain) {
        Certificate certificate = certificates.lockByIdAndOrganizationId(certificateId, organizationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificate not found"));
        if (certificate.getLifecycle() == CertificateLifecycle.ISSUED
            || certificate.getLifecycle() == CertificateLifecycle.ISSUING) {
            return new Prepared(snapshot(certificate), false);
        }

        String hash = hashes.hash(certificate);
        long expiresAt = expiresAt(certificate.getExpiryDate());
        if (expiresAt != 0 && expiresAt <= clock.instant().getEpochSecond()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Certificate expiry is in the past");
        }
        certificate.beginIssuance(hash, CertificateHashService.VERSION);
        BlockchainTransaction tx = transactions.saveAndFlush(new BlockchainTransaction(certificate,
            BlockchainTransactionType.ISSUE, chain.network(), chain.chainId(), chain.contractAddress()));
        certificates.flush();
        return new Prepared(snapshot(certificate, tx), true);
    }

    @Transactional
    public void submitted(UUID certificateId, UUID transactionId, String hash) {
        Certificate certificate = certificates.lockById(certificateId).orElseThrow();
        BlockchainTransaction tx = transactions.findById(transactionId).orElseThrow();
        if (certificate.getLifecycle() != CertificateLifecycle.ISSUING) return;
        if (tx.getStatus() == BlockchainTransactionStatus.SUBMITTED
            && hash.equalsIgnoreCase(tx.getTransactionHash())) return;
        tx.submitted(hash, clock.instant());
        transactions.flush();
    }

    @Transactional
    public void recoveredSubmission(UUID certificateId, UUID transactionId, String hash) {
        Certificate certificate = certificates.lockById(certificateId).orElseThrow();
        BlockchainTransaction tx = transactions.findById(transactionId).orElseThrow();
        if (certificate.getLifecycle() == CertificateLifecycle.ISSUED) return;
        if (tx.getTransactionHash() == null) tx.recoveredSubmission(hash, clock.instant());
        if (certificate.getLifecycle() == CertificateLifecycle.ISSUE_FAILED) {
            certificate.beginIssuance(certificate.getCertificateHash(), certificate.getCanonicalizationVersion());
        }
        transactions.flush();
    }

    @Transactional
    public void confirmed(UUID certificateId, UUID transactionId, IssueReceipt receipt) {
        Certificate certificate = certificates.lockById(certificateId).orElseThrow();
        BlockchainTransaction tx = transactions.findById(transactionId).orElseThrow();
        if (certificate.getLifecycle() == CertificateLifecycle.ISSUED) return;
        if (certificate.getLifecycle() == CertificateLifecycle.ISSUE_FAILED) {
            certificate.beginIssuance(certificate.getCertificateHash(), certificate.getCanonicalizationVersion());
        }
        if (tx.getStatus() == BlockchainTransactionStatus.FAILED) tx.reopenAfterValidation();
        tx.confirmed(receipt.blockNumber(), receipt.blockTimestamp(), clock.instant());
        certificate.markIssued(receipt.blockTimestamp());
        transactions.flush();
        certificates.flush();
    }

    @Transactional
    public void failed(UUID certificateId, UUID transactionId, String reason) {
        Certificate certificate = certificates.lockById(certificateId).orElseThrow();
        BlockchainTransaction tx = transactions.findById(transactionId).orElseThrow();
        if (certificate.getLifecycle() == CertificateLifecycle.ISSUED
            || tx.getStatus() == BlockchainTransactionStatus.CONFIRMED) return;
        tx.failed(reason);
        if (certificate.getLifecycle() == CertificateLifecycle.ISSUING) certificate.markIssueFailed();
        transactions.flush();
        certificates.flush();
    }

    @Transactional(readOnly = true)
    public List<UUID> unresolvedCertificateIds() {
        return transactions.findUnresolvedIssues(BlockchainTransactionType.ISSUE, CertificateLifecycle.ISSUED,
            Set.of(BlockchainTransactionStatus.CREATED,
            BlockchainTransactionStatus.SUBMITTED), PageRequest.of(0, 100))
            .stream().map(tx -> tx.getCertificate().getId()).distinct().toList();
    }

    private Snapshot snapshot(Certificate certificate) {
        BlockchainTransaction tx = transactions
            .findFirstByCertificateIdAndTransactionTypeOrderByCreatedAtDesc(
                certificate.getId(), BlockchainTransactionType.ISSUE).orElse(null);
        return snapshot(certificate, tx);
    }

    private Snapshot snapshot(Certificate certificate, BlockchainTransaction tx) {
        return new Snapshot(certificate.getId(), certificate.getOrganization().getId(),
            certificate.getCertificateId(), certificate.getLifecycle(), certificate.getCertificateHash(),
            hashes.contractKey(certificate.getCertificateId()), expiresAt(certificate.getExpiryDate()),
            tx == null ? null : tx.getId(), tx == null ? null : tx.getStatus(),
            tx == null ? null : tx.getTransactionHash(),
            tx == null ? null : tx.getBlockNumber(), tx == null ? null : tx.getBlockTimestamp(),
            tx == null ? null : tx.getFailureReason());
    }

    private static long expiresAt(LocalDate expiryDate) {
        return expiryDate == null ? 0 : expiryDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond() - 1;
    }

    public record Snapshot(UUID certificateId, UUID organizationId, String publicId,
                           CertificateLifecycle lifecycle, String certificateHash,
                           String certificateKey, long expiresAt, UUID transactionId,
                           BlockchainTransactionStatus transactionStatus, String transactionHash,
                           Long blockNumber, Instant blockTimestamp, String failureReason) {}
    public record Prepared(Snapshot snapshot, boolean created) {}
}
