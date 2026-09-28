package com.certchain.certificate;

import com.certchain.blockchain.BlockchainUnavailableException;
import com.certchain.blockchain.CertificateRegistryGateway;
import com.certchain.blockchain.CertificateRegistryGateway.ChainIdentity;
import com.certchain.blockchain.CertificateRegistryGateway.IssueReceipt;
import com.certchain.blockchain.InvalidBlockchainReceiptException;
import com.certchain.blockchain.IssueReceiptValidator;
import com.certchain.certificate.IssuancePersistence.Snapshot;
import com.certchain.transaction.BlockchainTransactionStatus;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class IssuanceService {
    private final CertificateRegistryGateway gateway;
    private final IssuancePersistence journal;
    private final IssueReceiptValidator validator;
    private final int confirmations;
    private final Duration receiptTimeout;

    public IssuanceService(CertificateRegistryGateway gateway, IssuancePersistence journal,
                           IssueReceiptValidator validator,
                           @Value("${app.blockchain.confirmations}") int confirmations,
                           @Value("${app.blockchain.receipt-timeout}") Duration receiptTimeout) {
        this.gateway = gateway;
        this.journal = journal;
        this.validator = validator;
        this.confirmations = confirmations;
        this.receiptTimeout = receiptTimeout;
    }

    public IssueProgress issue(UUID certificateId, UUID organizationId) {
        Snapshot current = journal.snapshot(certificateId, organizationId);
        ChainIdentity chain = gateway.identity();
        if (current.lifecycle() == CertificateLifecycle.ISSUED) return progress(current, chain);

        if (current.lifecycle() == CertificateLifecycle.ISSUING
            || current.lifecycle() == CertificateLifecycle.ISSUE_FAILED) {
            current = reconcileSnapshot(current, chain);
            if (current.lifecycle() == CertificateLifecycle.ISSUED
                || current.lifecycle() == CertificateLifecycle.ISSUING) return progress(current, chain);
        }

        // A failed attempt can only be retried after checking the chain. A proof without a
        // validated event is never submitted again or silently promoted to ISSUED.
        if (gateway.findCertificate(current.certificateKey()).isPresent()) {
            return progress(current, chain);
        }
        IssuancePersistence.Prepared prepared = journal.begin(certificateId, organizationId, chain);
        Snapshot attempt = prepared.snapshot();
        if (!prepared.created() || attempt.lifecycle() != CertificateLifecycle.ISSUING
            || attempt.transactionId() == null
            || attempt.transactionStatus() != BlockchainTransactionStatus.CREATED) {
            return progress(attempt, chain);
        }

        String transactionHash;
        try {
            transactionHash = gateway.submitIssue(attempt.certificateKey(), "0x" + attempt.certificateHash(),
                attempt.expiresAt());
        } catch (BlockchainUnavailableException failure) {
            // The RPC may have accepted the transaction before losing the response.
            // Keep CREATED/ISSUING so reconciliation searches by deterministic key.
            return progress(journal.snapshot(certificateId, organizationId), chain);
        }
        journal.submitted(certificateId, attempt.transactionId(), transactionHash);
        return progress(waitForReceipt(journal.snapshot(certificateId, organizationId), chain), chain);
    }

    public IssueProgress reconcile(UUID certificateId, UUID organizationId) {
        Snapshot snapshot = journal.snapshot(certificateId, organizationId);
        ChainIdentity chain = gateway.identity();
        return progress(reconcileSnapshot(snapshot, chain), chain);
    }

    public IssueProgress progress(UUID certificateId, UUID organizationId) {
        Snapshot snapshot = journal.snapshot(certificateId, organizationId);
        return progress(snapshot, gateway.identity());
    }

    public void reconcileBackground(UUID certificateId) {
        // Resolve the tenant from the stored certificate, never from an external request.
        UUID organizationId = journal.organizationId(certificateId);
        reconcile(certificateId, organizationId);
    }

    private Snapshot waitForReceipt(Snapshot snapshot, ChainIdentity chain) {
        long deadline = System.nanoTime() + receiptTimeout.toNanos();
        Snapshot current = snapshot;
        do {
            current = reconcileSnapshot(current, chain);
            if (current.lifecycle() != CertificateLifecycle.ISSUING) return current;
            if (System.nanoTime() >= deadline) return current;
            try { Thread.sleep(Math.min(1000, Math.max(1, receiptTimeout.toMillis()))); }
            catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return current;
            }
        } while (true);
    }

    private Snapshot reconcileSnapshot(Snapshot snapshot, ChainIdentity chain) {
        if (snapshot.lifecycle() == CertificateLifecycle.ISSUED || snapshot.transactionId() == null) return snapshot;
        try {
            Optional<IssueReceipt> receipt;
            if (snapshot.transactionHash() == null) {
                receipt = gateway.findIssueByKey(snapshot.certificateKey());
                if (receipt.isEmpty()) return snapshot;
                journal.recoveredSubmission(snapshot.certificateId(), snapshot.transactionId(),
                    receipt.orElseThrow().transactionHash());
                snapshot = journal.snapshot(snapshot.certificateId(), snapshot.organizationId());
            } else {
                receipt = gateway.findReceipt(snapshot.transactionHash());
            }
            if (receipt.isEmpty()) return snapshot;
            IssueReceipt found = receipt.orElseThrow();
            boolean confirmed = validator.isConfirmed(found, chain, snapshot.transactionHash(),
                snapshot.certificateKey(), "0x" + snapshot.certificateHash(), snapshot.expiresAt(), confirmations);
            if (!confirmed) return snapshot;
            validator.validateRecord(gateway.findCertificate(snapshot.certificateKey()), chain,
                "0x" + snapshot.certificateHash(), snapshot.expiresAt(),
                found.events().getFirst().issuedAt());
            journal.confirmed(snapshot.certificateId(), snapshot.transactionId(), found);
        } catch (InvalidBlockchainReceiptException invalid) {
            journal.failed(snapshot.certificateId(), snapshot.transactionId(), invalid.getMessage());
        } catch (BlockchainUnavailableException unavailable) {
            // The journal remains CREATED or SUBMITTED for the next reconciliation pass.
        }
        return journal.snapshot(snapshot.certificateId(), snapshot.organizationId());
    }

    private IssueProgress progress(Snapshot snapshot, ChainIdentity chain) {
        String explorerUrl = snapshot.transactionHash() == null ? null : switch (chain.network()) {
            case "sepolia" -> "https://sepolia.etherscan.io/tx/" + snapshot.transactionHash();
            case "mainnet" -> "https://etherscan.io/tx/" + snapshot.transactionHash();
            default -> null;
        };
        String guidance = switch (snapshot.lifecycle()) {
            case DRAFT -> "Confirm issuance to submit this draft.";
            case ISSUING -> "Submission is pending. Reconcile before retrying.";
            case ISSUE_FAILED -> "Issuance failed or is uncertain. Reconcile before retrying.";
            case ISSUED -> "Proof confirmed on chain.";
        };
        return new IssueProgress(snapshot.certificateId(), snapshot.publicId(), snapshot.lifecycle(),
            snapshot.certificateHash(), snapshot.transactionStatus(), snapshot.transactionHash(),
            chain.network(), chain.chainId(), chain.contractAddress(), snapshot.blockNumber(),
            snapshot.blockTimestamp(), explorerUrl, snapshot.failureReason(), guidance);
    }

    public record IssueProgress(UUID id, String certificateId, CertificateLifecycle lifecycle,
                                String certificateHash, BlockchainTransactionStatus transactionStatus,
                                String transactionHash, String network, long chainId,
                                String contractAddress, Long blockNumber, java.time.Instant blockTimestamp,
                                String explorerUrl, String failureReason, String guidance) {}
}
