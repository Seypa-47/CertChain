package com.certchain.blockchain;

import com.certchain.blockchain.CertificateRegistryGateway.ChainIdentity;
import com.certchain.blockchain.CertificateRegistryGateway.RevokeReceipt;
import com.certchain.blockchain.CertificateRegistryGateway.OnChainCertificate;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class RevokeReceiptValidator {
    public boolean isConfirmed(RevokeReceipt receipt, ChainIdentity chain, String transactionHash,
                               String key, int requiredConfirmations) {
        if (!receipt.success()) throw new InvalidBlockchainReceiptException("Revocation transaction reverted");
        if (receipt.chainId() != chain.chainId() || !same(receipt.transactionHash(), transactionHash)
            || !same(receipt.to(), chain.contractAddress())) {
            throw new InvalidBlockchainReceiptException("Revocation chain or transaction mismatch");
        }
        if (receipt.blockNumber() < 1 || receipt.blockTimestamp() == null || receipt.confirmations() < 1) {
            throw new InvalidBlockchainReceiptException("Revocation block metadata missing");
        }
        if (receipt.events() == null || receipt.events().size() != 1) {
            throw new InvalidBlockchainReceiptException("Expected one revocation event");
        }
        var event = receipt.events().getFirst();
        if (!"CertificateRevoked".equals(event.name()) || !same(event.address(), chain.contractAddress())
            || !same(event.certificateKey(), key) || !same(event.revokedBy(), chain.issuerAddress())
            || event.revokedAt() != receipt.blockTimestamp().getEpochSecond()) {
            throw new InvalidBlockchainReceiptException("Revocation event mismatch");
        }
        return receipt.confirmations() >= requiredConfirmations;
    }

    public void validateRecord(Optional<OnChainCertificate> record) {
        if (record.isEmpty() || !record.orElseThrow().revoked()) {
            throw new InvalidBlockchainReceiptException("On-chain revocation missing");
        }
    }

    private static boolean same(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }
}
