package com.certchain.certificate;

import com.certchain.blockchain.CertificateRegistryGateway.ChainIdentity;
import com.certchain.blockchain.CertificateRegistryGateway.RevokeReceipt;
import com.certchain.transaction.BlockchainTransaction;
import com.certchain.transaction.BlockchainTransactionRepository;
import com.certchain.transaction.BlockchainTransactionStatus;
import com.certchain.transaction.BlockchainTransactionType;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RevocationPersistence {
    private final CertificateRepository certificates;
    private final BlockchainTransactionRepository transactions;
    private final CertificateHashService hashes;
    private final Clock clock;

    public RevocationPersistence(CertificateRepository certificates, BlockchainTransactionRepository transactions,
                                 CertificateHashService hashes, Clock clock) {
        this.certificates = certificates;
        this.transactions = transactions;
        this.hashes = hashes;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Snapshot snapshot(UUID id, UUID organizationId) {
        Certificate certificate = certificates.findByIdAndOrganizationId(id, organizationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificate not found"));
        return snapshot(certificate);
    }

    @Transactional(readOnly = true)
    public UUID organizationId(UUID id) {
        return certificates.findById(id).orElseThrow().getOrganization().getId();
    }

    @Transactional
    public Prepared begin(UUID id, UUID organizationId, String reason, ChainIdentity chain) {
        Certificate certificate = certificates.lockByIdAndOrganizationId(id, organizationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificate not found"));
        requireIssued(certificate);
        if (certificate.getRevokedAt() != null) throw conflict("CERTIFICATE_ALREADY_REVOKED", "Certificate is already revoked");
        Snapshot current = snapshot(certificate);
        if (current.transactionStatus() == BlockchainTransactionStatus.CREATED
            || current.transactionStatus() == BlockchainTransactionStatus.SUBMITTED) {
            return new Prepared(current, false);
        }
        certificate.setRevocationReason(reason);
        BlockchainTransaction transaction = transactions.saveAndFlush(new BlockchainTransaction(certificate,
            BlockchainTransactionType.REVOKE, chain.network(), chain.chainId(), chain.contractAddress()));
        certificates.flush();
        return new Prepared(snapshot(certificate, transaction), true);
    }

    @Transactional
    public void submitted(UUID certificateId, UUID transactionId, String hash) {
        Certificate certificate = certificates.lockById(certificateId).orElseThrow();
        BlockchainTransaction transaction = transactions.findById(transactionId).orElseThrow();
        if (certificate.getRevokedAt() != null) return;
        if (transaction.getStatus() == BlockchainTransactionStatus.SUBMITTED
            && hash.equalsIgnoreCase(transaction.getTransactionHash())) return;
        transaction.submitted(hash, clock.instant());
        transactions.flush();
    }

    @Transactional
    public void recovered(UUID certificateId, UUID transactionId, String hash) {
        Certificate certificate = certificates.lockById(certificateId).orElseThrow();
        BlockchainTransaction transaction = transactions.findById(transactionId).orElseThrow();
        if (certificate.getRevokedAt() != null) return;
        if (transaction.getTransactionHash() == null) transaction.recoveredSubmission(hash, clock.instant());
        transactions.flush();
    }

    @Transactional
    public void confirmed(UUID certificateId, UUID transactionId, RevokeReceipt receipt) {
        Certificate certificate = certificates.lockById(certificateId).orElseThrow();
        BlockchainTransaction transaction = transactions.findById(transactionId).orElseThrow();
        if (certificate.getRevokedAt() != null) return;
        if (transaction.getStatus() == BlockchainTransactionStatus.FAILED) transaction.reopenAfterValidation();
        transaction.confirmed(receipt.blockNumber(), receipt.blockTimestamp(), clock.instant());
        certificate.markRevoked(Instant.ofEpochSecond(receipt.events().getFirst().revokedAt()));
        transactions.flush();
        certificates.flush();
    }

    @Transactional
    public void failed(UUID certificateId, UUID transactionId, String reason) {
        Certificate certificate = certificates.lockById(certificateId).orElseThrow();
        BlockchainTransaction transaction = transactions.findById(transactionId).orElseThrow();
        if (certificate.getRevokedAt() != null || transaction.getStatus() == BlockchainTransactionStatus.CONFIRMED) return;
        transaction.failed(reason);
        transactions.flush();
    }

    @Transactional(readOnly = true)
    public List<UUID> unresolvedIds() {
        return transactions.findUnresolvedRevocations(BlockchainTransactionType.REVOKE,
            Set.of(BlockchainTransactionStatus.CREATED, BlockchainTransactionStatus.SUBMITTED),
            PageRequest.of(0, 100)).stream().map(tx -> tx.getCertificate().getId()).distinct().toList();
    }

    private Snapshot snapshot(Certificate certificate) {
        BlockchainTransaction transaction = transactions
            .findFirstByCertificateIdAndTransactionTypeOrderByCreatedAtDesc(
                certificate.getId(), BlockchainTransactionType.REVOKE).orElse(null);
        return snapshot(certificate, transaction);
    }

    private Snapshot snapshot(Certificate certificate, BlockchainTransaction transaction) {
        return new Snapshot(certificate.getId(), certificate.getOrganization().getId(),
            certificate.getCertificateId(), certificate.getLifecycle(), certificate.getRevokedAt(),
            hashes.contractKey(certificate.getCertificateId()), certificate.getCertificateHash(),
            transaction == null ? null : transaction.getId(),
            transaction == null ? null : transaction.getStatus(),
            transaction == null ? null : transaction.getTransactionHash(),
            transaction == null ? null : transaction.getBlockNumber(),
            transaction == null ? null : transaction.getBlockTimestamp(),
            transaction == null ? null : transaction.getFailureReason());
    }

    private static void requireIssued(Certificate certificate) {
        if (certificate.getLifecycle() != CertificateLifecycle.ISSUED) {
            throw conflict("CERTIFICATE_NOT_ISSUED", "Only issued certificates may be revoked");
        }
    }

    private static CertificateConflictException conflict(String code, String message) {
        return new CertificateConflictException(code, message);
    }

    public record Snapshot(UUID id, UUID organizationId, String certificateId,
                           CertificateLifecycle lifecycle, Instant revokedAt,
                           String key, String hash, UUID transactionId,
                           BlockchainTransactionStatus transactionStatus, String transactionHash,
                           Long blockNumber, Instant blockTimestamp, String failureReason) {}
    public record Prepared(Snapshot snapshot, boolean created) {}
}
