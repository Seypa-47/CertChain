package com.certchain.blockchain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CertificateRegistryGateway {
    ChainIdentity identity();
    Optional<OnChainCertificate> findCertificate(String certificateKey);
    String submitIssue(String certificateKey, String certificateHash, long expiresAt);
    Optional<IssueReceipt> findReceipt(String transactionHash);
    Optional<IssueReceipt> findIssueByKey(String certificateKey);
    String submitRevoke(String certificateKey);
    Optional<RevokeReceipt> findRevokeReceipt(String transactionHash);
    Optional<RevokeReceipt> findRevokeByKey(String certificateKey);

    record ChainIdentity(String network, long chainId, String contractAddress, String issuerAddress) {}
    record OnChainCertificate(String certificateHash, long issuedAt, long expiresAt,
                              boolean revoked, String issuer) {}
    record IssueEvent(String name, String certificateKey, String certificateHash,
                      String issuer, long issuedAt, long expiresAt, String address) {}
    record IssueReceipt(String transactionHash, boolean success, long chainId,
                        String to, long blockNumber, Instant blockTimestamp,
                        long confirmations, List<IssueEvent> events) {}
    record RevokeEvent(String name, String certificateKey, String revokedBy,
                       long revokedAt, String address) {}
    record RevokeReceipt(String transactionHash, boolean success, long chainId,
                         String to, long blockNumber, Instant blockTimestamp,
                         long confirmations, List<RevokeEvent> events) {}
}
