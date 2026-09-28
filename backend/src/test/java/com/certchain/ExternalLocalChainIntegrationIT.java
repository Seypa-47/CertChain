package com.certchain;

import com.certchain.blockchain.CertificateRegistryGateway;
import com.certchain.blockchain.BlockchainSettings;
import com.certchain.blockchain.Web3jCertificateRegistryGateway;
import com.certchain.certificate.Certificate;
import com.certchain.certificate.CertificateHashService;
import com.certchain.certificate.CertificateIdGenerator;
import com.certchain.certificate.CertificateLifecycle;
import com.certchain.certificate.CertificateRepository;
import com.certchain.certificate.IssuanceService;
import com.certchain.certificate.PublicVerificationController;
import com.certchain.certificate.RevocationService;
import com.certchain.organization.Organization;
import com.certchain.organization.OrganizationRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;

/** Run explicitly against an isolated PostgreSQL database and a disposable local Hardhat chain. */
@SpringBootTest(properties = {
    "app.auth.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
    "app.blockchain.enabled=true", "app.blockchain.network=local",
    "app.blockchain.chain-id=31337", "app.blockchain.confirmations=1",
    "app.blockchain.receipt-timeout=PT3S"
})
@EnabledIfEnvironmentVariable(named = "CERTCHAIN_LOCAL_RPC", matches = ".+")
class ExternalLocalChainIntegrationIT {
    @DynamicPropertySource
    static void blockchain(DynamicPropertyRegistry properties) {
        properties.add("app.blockchain.rpc-url", () -> System.getenv("CERTCHAIN_LOCAL_RPC"));
        properties.add("app.blockchain.contract-address", () -> System.getenv("CERTCHAIN_LOCAL_CONTRACT"));
        properties.add("app.blockchain.issuer-private-key", () -> System.getenv("CERTCHAIN_LOCAL_ISSUER_KEY"));
    }

    @Autowired OrganizationRepository organizations;
    @Autowired CertificateRepository certificates;
    @Autowired CertificateIdGenerator ids;
    @Autowired CertificateRegistryGateway gateway;
    @Autowired CertificateHashService hashes;
    @Autowired IssuanceService issuance;
    @Autowired RevocationService revocations;
    @Autowired PublicVerificationController verification;
    @Autowired JdbcTemplate jdbc;
    @Autowired Clock clock;

    private Certificate draft() {
        Organization org = organizations.saveAndFlush(new Organization("Local test", UUID.randomUUID() + "@example.test"));
        return certificates.saveAndFlush(new Certificate(ids.nextId(), org, "Synthetic Recipient",
            "recipient@example.test", "Synthetic Workshop", LocalDate.now(clock)));
    }

    @Test
    void issuedProofVerifiesAndRevocationTakesPriorityWithoutDuplicateSubmission() {
        Certificate certificate = draft();
        UUID organizationId = certificate.getOrganization().getId();
        var issued = issuance.issue(certificate.getId(), organizationId);
        assertEquals(CertificateLifecycle.ISSUED, issued.lifecycle());
        assertEquals(PublicVerificationController.ProofResult.VERIFIED,
            verification.verify(certificate.getCertificateId()).proofResult());
        assertEquals("VALID", verification.verify(certificate.getCertificateId()).status().name());
        assertEquals(issued.transactionHash(), issuance.issue(certificate.getId(), organizationId).transactionHash());

        var revoked = revocations.revoke(certificate.getId(), organizationId, "Synthetic test reason");
        assertNotNull(revoked.revokedAt());
        var publicResult = verification.verify(certificate.getCertificateId());
        assertEquals("REVOKED", publicResult.status().name());
        assertTrue(publicResult.blockchainVerified());
        assertFalse(publicResult.toString().contains("Synthetic test reason"));
        assertThrows(com.certchain.certificate.CertificateConflictException.class,
            () -> revocations.revoke(certificate.getId(), organizationId, "again"));
    }

    @Test
    void databaseTamperingFailsProofAndRestoringDataRecoversVerification() {
        Certificate certificate = draft();
        assertEquals(CertificateLifecycle.ISSUED,
            issuance.issue(certificate.getId(), certificate.getOrganization().getId()).lifecycle());
        String id = certificate.getCertificateId();
        jdbc.update("update certificate set program_name = 'Tampered' where id = ?", certificate.getId());
        try {
            var mismatch = verification.verify(id);
            assertEquals(PublicVerificationController.ProofResult.PROOF_MISMATCH, mismatch.proofResult());
            assertFalse(mismatch.blockchainVerified());
            assertNull(mismatch.status());
        } finally {
            jdbc.update("update certificate set program_name = 'Synthetic Workshop' where id = ?", certificate.getId());
        }
        assertEquals(PublicVerificationController.ProofResult.VERIFIED,
            verification.verify(id).proofResult());
    }

    @Test
    void duplicateOnChainSubmissionIsRejectedAndWrongChainConfigurationFails() {
        Certificate certificate = draft();
        var issued = issuance.issue(certificate.getId(), certificate.getOrganization().getId());
        assertEquals(CertificateLifecycle.ISSUED, issued.lifecycle());
        String key = hashes.contractKey(certificate.getCertificateId());
        try {
            String duplicateHash = gateway.submitIssue(key, "0x" + issued.certificateHash(), 0);
            assertFalse(gateway.findReceipt(duplicateHash).orElseThrow().success());
        } catch (com.certchain.blockchain.BlockchainUnavailableException rejectedBeforeMining) {
            assertEquals(CertificateLifecycle.ISSUED,
                certificates.findById(certificate.getId()).orElseThrow().getLifecycle());
        }
        BlockchainSettings wrong = new BlockchainSettings("local", 1, System.getenv("CERTCHAIN_LOCAL_RPC"),
            System.getenv("CERTCHAIN_LOCAL_CONTRACT"), System.getenv("CERTCHAIN_LOCAL_ISSUER_KEY"),
            1, 0, Duration.ofSeconds(1));
        assertThrows(IllegalStateException.class, () -> new Web3jCertificateRegistryGateway(wrong));
    }
}
