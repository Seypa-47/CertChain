package com.certchain.certificate;

import com.certchain.blockchain.BlockchainUnavailableException;
import com.certchain.blockchain.CertificateRegistryGateway;
import com.certchain.blockchain.CertificateRegistryGateway.ChainIdentity;
import com.certchain.blockchain.CertificateRegistryGateway.RevokeReceipt;
import com.certchain.blockchain.InvalidBlockchainReceiptException;
import com.certchain.blockchain.RevokeReceiptValidator;
import com.certchain.certificate.RevocationPersistence.Snapshot;
import com.certchain.transaction.BlockchainTransactionStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RevocationService {
    private final CertificateRegistryGateway gateway;
    private final RevocationPersistence journal;
    private final RevokeReceiptValidator validator;
    private final int confirmations;
    private final Duration receiptTimeout;

    public RevocationService(CertificateRegistryGateway gateway, RevocationPersistence journal,
                             RevokeReceiptValidator validator,
                             @Value("${app.blockchain.confirmations}") int confirmations,
                             @Value("${app.blockchain.receipt-timeout}") Duration receiptTimeout) {
        this.gateway = gateway;
        this.journal = journal;
        this.validator = validator;
        this.confirmations = confirmations;
        this.receiptTimeout = receiptTimeout;
    }

    public RevokeProgress revoke(UUID id, UUID organizationId, String suppliedReason) {
        Snapshot current = journal.snapshot(id, organizationId);
        requireRevocable(current);
        String reason = suppliedReason == null ? "" : suppliedReason.trim();
        if (reason.isBlank() || reason.length() > 1000) {
            throw new CertificateConflictException("REVOCATION_REASON_INVALID", "A reason of 1 to 1000 characters is required");
        }
        ChainIdentity chain = gateway.identity();
        if (current.transactionId() != null) {
            current = reconcileSnapshot(current, chain);
            if (current.revokedAt() != null) return progress(current, chain);
            if (current.transactionStatus() == BlockchainTransactionStatus.CREATED
                || current.transactionStatus() == BlockchainTransactionStatus.SUBMITTED) return progress(current, chain);
        }
        var record = gateway.findCertificate(current.key());
        if (record.isEmpty() || !record.orElseThrow().certificateHash().equalsIgnoreCase("0x" + current.hash())) {
            throw new CertificateConflictException("CHAIN_PROOF_MISMATCH", "Issued chain proof is unavailable or differs");
        }
        if (record.orElseThrow().revoked()) {
            throw new CertificateConflictException("REVOCATION_REQUIRES_RECONCILIATION",
                "On-chain revocation needs a validated event before local confirmation");
        }
        var prepared = journal.begin(id, organizationId, reason, chain);
        Snapshot attempt = prepared.snapshot();
        if (!prepared.created()) return progress(attempt, chain);
        String transactionHash;
        try {
            transactionHash = gateway.submitRevoke(attempt.key());
        } catch (BlockchainUnavailableException uncertain) {
            // A lost RPC response can hide a submitted transaction. Keep CREATED for event recovery.
            return progress(journal.snapshot(id, organizationId), chain);
        }
        journal.submitted(id, attempt.transactionId(), transactionHash);
        long deadline = System.nanoTime() + receiptTimeout.toNanos();
        do {
            Snapshot checked = reconcileSnapshot(journal.snapshot(id, organizationId), chain);
            if (checked.revokedAt() != null || checked.transactionStatus() == BlockchainTransactionStatus.FAILED
                || System.nanoTime() >= deadline) return progress(checked, chain);
            try { Thread.sleep(Math.min(1000, Math.max(1, receiptTimeout.toMillis()))); }
            catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return progress(checked, chain);
            }
        } while (true);
    }

    public RevokeProgress reconcile(UUID id, UUID organizationId) {
        Snapshot snapshot = journal.snapshot(id, organizationId);
        ChainIdentity chain = gateway.identity();
        return progress(reconcileSnapshot(snapshot, chain), chain);
    }

    public RevokeProgress progress(UUID id, UUID organizationId) {
        Snapshot snapshot = journal.snapshot(id, organizationId);
        return progress(snapshot, gateway.identity());
    }

    public void reconcileBackground(UUID id) {
        reconcile(id, journal.organizationId(id));
    }

    private Snapshot reconcileSnapshot(Snapshot snapshot, ChainIdentity chain) {
        if (snapshot.revokedAt() != null || snapshot.transactionId() == null) return snapshot;
        try {
            Optional<RevokeReceipt> receipt;
            if (snapshot.transactionHash() == null) {
                receipt = gateway.findRevokeByKey(snapshot.key());
                if (receipt.isEmpty()) return snapshot;
                journal.recovered(snapshot.id(), snapshot.transactionId(), receipt.orElseThrow().transactionHash());
                snapshot = journal.snapshot(snapshot.id(), snapshot.organizationId());
            } else {
                receipt = gateway.findRevokeReceipt(snapshot.transactionHash());
            }
            if (receipt.isEmpty()) return snapshot;
            RevokeReceipt found = receipt.orElseThrow();
            if (!validator.isConfirmed(found, chain, snapshot.transactionHash(), snapshot.key(), confirmations)) {
                return snapshot;
            }
            validator.validateRecord(gateway.findCertificate(snapshot.key()));
            journal.confirmed(snapshot.id(), snapshot.transactionId(), found);
        } catch (InvalidBlockchainReceiptException invalid) {
            journal.failed(snapshot.id(), snapshot.transactionId(), invalid.getMessage());
        } catch (BlockchainUnavailableException unavailable) {
            // Keep the journal pending for later reconciliation.
        }
        return journal.snapshot(snapshot.id(), snapshot.organizationId());
    }

    private static void requireRevocable(Snapshot snapshot) {
        if (snapshot.lifecycle() != CertificateLifecycle.ISSUED) {
            throw new CertificateConflictException("CERTIFICATE_NOT_ISSUED", "Only issued certificates may be revoked");
        }
        if (snapshot.revokedAt() != null) {
            throw new CertificateConflictException("CERTIFICATE_ALREADY_REVOKED", "Certificate is already revoked");
        }
    }

    private RevokeProgress progress(Snapshot snapshot, ChainIdentity chain) {
        String explorer = snapshot.transactionHash() == null ? null : switch (chain.network()) {
            case "sepolia" -> "https://sepolia.etherscan.io/tx/" + snapshot.transactionHash();
            case "mainnet" -> "https://etherscan.io/tx/" + snapshot.transactionHash();
            default -> null;
        };
        String guidance = snapshot.revokedAt() != null ? "Revocation confirmed on chain."
            : snapshot.transactionStatus() == BlockchainTransactionStatus.FAILED
                ? "Revocation failed. Reconcile before retrying."
                : snapshot.transactionStatus() == BlockchainTransactionStatus.CREATED
                    || snapshot.transactionStatus() == BlockchainTransactionStatus.SUBMITTED
                    ? "Revocation is pending. Reconcile before retrying."
                    : "Confirm revocation to submit the transaction.";
        return new RevokeProgress(snapshot.id(), snapshot.certificateId(), snapshot.revokedAt(),
            snapshot.transactionStatus(), snapshot.transactionHash(), chain.network(), chain.chainId(),
            chain.contractAddress(), snapshot.blockNumber(), snapshot.blockTimestamp(), explorer,
            snapshot.failureReason(), guidance);
    }

    public record RevokeProgress(UUID id, String certificateId, Instant revokedAt,
                                 BlockchainTransactionStatus transactionStatus, String transactionHash,
                                 String network, long chainId, String contractAddress, Long blockNumber,
                                 Instant blockTimestamp, String explorerUrl, String failureReason,
                                 String guidance) {}
}
