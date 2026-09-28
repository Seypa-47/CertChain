package com.certchain.blockchain;

import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.blockchain", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DisabledCertificateRegistryGateway implements CertificateRegistryGateway {
    private BlockchainUnavailableException disabled() {
        return new BlockchainUnavailableException("Blockchain integration is disabled");
    }
    @Override public ChainIdentity identity() { throw disabled(); }
    @Override public Optional<OnChainCertificate> findCertificate(String key) { throw disabled(); }
    @Override public String submitIssue(String key, String hash, long expiry) { throw disabled(); }
    @Override public Optional<IssueReceipt> findReceipt(String hash) { throw disabled(); }
    @Override public Optional<IssueReceipt> findIssueByKey(String key) { throw disabled(); }
}
