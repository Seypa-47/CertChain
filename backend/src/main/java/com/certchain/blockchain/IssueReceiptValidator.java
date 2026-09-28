package com.certchain.blockchain;

import com.certchain.blockchain.CertificateRegistryGateway.ChainIdentity;
import com.certchain.blockchain.CertificateRegistryGateway.IssueEvent;
import com.certchain.blockchain.CertificateRegistryGateway.IssueReceipt;
import com.certchain.blockchain.CertificateRegistryGateway.OnChainCertificate;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class IssueReceiptValidator {
    public boolean isConfirmed(IssueReceipt receipt, ChainIdentity chain, String transactionHash,
                               String key, String hash, long expiresAt, int requiredConfirmations) {
        if (!receipt.success()) throw new InvalidBlockchainReceiptException("Transaction reverted");
        if (receipt.chainId() != chain.chainId()) throw new InvalidBlockchainReceiptException("Wrong chain");
        if (!same(receipt.transactionHash(), transactionHash)) throw new InvalidBlockchainReceiptException("Wrong transaction");
        if (!same(receipt.to(), chain.contractAddress())) throw new InvalidBlockchainReceiptException("Wrong contract");
        if (receipt.blockNumber() < 1 || receipt.blockTimestamp() == null || receipt.confirmations() < 1) {
            throw new InvalidBlockchainReceiptException("Missing block metadata");
        }
        if (receipt.events() == null || receipt.events().size() != 1) {
            throw new InvalidBlockchainReceiptException("Expected one issuance event");
        }
        IssueEvent event = receipt.events().getFirst();
        if (!"CertificateIssued".equals(event.name()) || !same(event.address(), chain.contractAddress())
            || !same(event.certificateKey(), key) || !same(event.certificateHash(), hash)
            || !same(event.issuer(), chain.issuerAddress()) || event.expiresAt() != expiresAt
            || event.issuedAt() != receipt.blockTimestamp().getEpochSecond()) {
            throw new InvalidBlockchainReceiptException("Issuance event mismatch");
        }
        return receipt.confirmations() >= requiredConfirmations;
    }

    public void validateRecord(Optional<OnChainCertificate> record, ChainIdentity chain,
                               String hash, long expiresAt, long issuedAt) {
        if (record.isEmpty()) throw new InvalidBlockchainReceiptException("On-chain proof missing");
        OnChainCertificate actual = record.orElseThrow();
        if (!same(actual.certificateHash(), hash) || !same(actual.issuer(), chain.issuerAddress())
            || actual.expiresAt() != expiresAt || actual.issuedAt() != issuedAt || actual.revoked()) {
            throw new InvalidBlockchainReceiptException("On-chain proof mismatch");
        }
    }

    private static boolean same(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }
}
