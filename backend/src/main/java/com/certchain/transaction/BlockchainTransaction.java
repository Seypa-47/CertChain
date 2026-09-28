package com.certchain.transaction;

import com.certchain.certificate.Certificate;
import com.certchain.common.domain.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "blockchain_transaction")
public class BlockchainTransaction extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "certificate_id", nullable = false)
    private Certificate certificate;
    @Column(name = "transaction_hash", nullable = true, length = 66)
    private String transactionHash;
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 12)
    private BlockchainTransactionType transactionType;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    private BlockchainTransactionStatus status;
    @Column(name = "network", nullable = false, length = 64)
    private String network;
    @Column(name = "chain_id", nullable = false)
    private long chainId;
    @Column(name = "contract_address", nullable = false, length = 42)
    private String contractAddress;
    @Column(name = "block_number", nullable = true)
    private Long blockNumber;
    @Column(name = "block_timestamp", nullable = true)
    private Instant blockTimestamp;
    @Column(columnDefinition = "text")
    private String failureReason;
    @Column(name = "submitted_at", nullable = true)
    private Instant submittedAt;
    @Column(name = "confirmed_at", nullable = true)
    private Instant confirmedAt;

    protected BlockchainTransaction() {}
    public BlockchainTransaction(Certificate certificate, BlockchainTransactionType transactionType, String network, long chainId, String contractAddress) {
        this.certificate = java.util.Objects.requireNonNull(certificate);
        this.transactionType = java.util.Objects.requireNonNull(transactionType);
        this.network = java.util.Objects.requireNonNull(network);
        this.chainId = chainId;
        this.contractAddress = java.util.Objects.requireNonNull(contractAddress);
        this.status = BlockchainTransactionStatus.CREATED;
    }

    public Certificate getCertificate() { return certificate; }
    public String getTransactionHash() { return transactionHash; }
    public BlockchainTransactionType getTransactionType() { return transactionType; }
    public BlockchainTransactionStatus getStatus() { return status; }
    public String getNetwork() { return network; }
    public long getChainId() { return chainId; }
    public String getContractAddress() { return contractAddress; }
    public Long getBlockNumber() { return blockNumber; }
    public Instant getBlockTimestamp() { return blockTimestamp; }
    public String getFailureReason() { return failureReason; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public UUID getId() { return id; }
    public void setTransactionHash(String value) { this.transactionHash = value; }
    public void setStatus(BlockchainTransactionStatus value) { this.status = value; }
    public void setBlockNumber(Long value) { this.blockNumber = value; }
    public void setBlockTimestamp(Instant value) { this.blockTimestamp = value; }
    public void setFailureReason(String value) { this.failureReason = value; }
    public void setSubmittedAt(Instant value) { this.submittedAt = value; }
    public void setConfirmedAt(Instant value) { this.confirmedAt = value; }

    public void submitted(String hash, Instant when) {
        if (status != BlockchainTransactionStatus.CREATED || transactionHash != null) {
            throw new IllegalStateException("Transaction is not awaiting submission");
        }
        transactionHash = java.util.Objects.requireNonNull(hash);
        submittedAt = java.util.Objects.requireNonNull(when);
        status = BlockchainTransactionStatus.SUBMITTED;
    }

    public void confirmed(long number, Instant blockTime, Instant when) {
        if (status != BlockchainTransactionStatus.SUBMITTED) {
            throw new IllegalStateException("Transaction is not submitted");
        }
        blockNumber = number;
        blockTimestamp = java.util.Objects.requireNonNull(blockTime);
        confirmedAt = java.util.Objects.requireNonNull(when);
        failureReason = null;
        status = BlockchainTransactionStatus.CONFIRMED;
    }

    public void failed(String reason) {
        if (status == BlockchainTransactionStatus.CONFIRMED) {
            throw new IllegalStateException("Confirmed transaction cannot fail");
        }
        failureReason = java.util.Objects.requireNonNull(reason);
        status = BlockchainTransactionStatus.FAILED;
    }

    public void recoveredSubmission(String hash, Instant when) {
        if (transactionHash != null || (status != BlockchainTransactionStatus.CREATED
            && status != BlockchainTransactionStatus.FAILED)) {
            throw new IllegalStateException("Transaction cannot be recovered");
        }
        transactionHash = java.util.Objects.requireNonNull(hash);
        submittedAt = java.util.Objects.requireNonNull(when);
        failureReason = null;
        status = BlockchainTransactionStatus.SUBMITTED;
    }

    public void reopenAfterValidation() {
        if (status != BlockchainTransactionStatus.FAILED || transactionHash == null) {
            throw new IllegalStateException("Failed transaction has no recoverable hash");
        }
        failureReason = null;
        status = BlockchainTransactionStatus.SUBMITTED;
    }
}
