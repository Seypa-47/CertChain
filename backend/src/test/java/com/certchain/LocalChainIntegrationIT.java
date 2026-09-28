package com.certchain;

import com.certchain.blockchain.BlockchainSettings;
import com.certchain.blockchain.CertificateRegistryGateway;
import com.certchain.blockchain.IssueReceiptValidator;
import com.certchain.blockchain.Web3jCertificateRegistryGateway;
import com.certchain.certificate.Certificate;
import com.certchain.certificate.CertificateHashService;
import com.certchain.certificate.CertificateLifecycle;
import com.certchain.certificate.CertificateRepository;
import com.certchain.certificate.IssuancePersistence;
import com.certchain.certificate.IssuanceService;
import com.certchain.organization.Organization;
import com.certchain.organization.OrganizationRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import static org.junit.jupiter.api.Assertions.*;

/** Run explicitly with -Dtest=LocalChainIntegrationIT and local-chain environment variables. */
@SpringBootTest(properties = {
    "app.auth.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
    "app.blockchain.enabled=true", "app.blockchain.network=local",
    "app.blockchain.chain-id=31337", "app.blockchain.confirmations=1",
    "app.blockchain.receipt-timeout=PT0.2S"
})
@Testcontainers
class LocalChainIntegrationIT {
    @Container @ServiceConnection
    static final PostgreSQLContainer postgres =
        new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " is required for local-chain tests");
        return value;
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry properties) {
        properties.add("app.blockchain.rpc-url", () -> required("CERTCHAIN_LOCAL_RPC"));
        properties.add("app.blockchain.contract-address", () -> required("CERTCHAIN_LOCAL_CONTRACT"));
        properties.add("app.blockchain.issuer-private-key", () -> required("CERTCHAIN_LOCAL_ISSUER_KEY"));
    }

    @Autowired CertificateRegistryGateway gateway;
    @Autowired IssueReceiptValidator validator;
    @Autowired IssuanceService issuance;
    @Autowired IssuancePersistence journal;
    @Autowired CertificateHashService hashes;
    @Autowired CertificateRepository certificates;
    @Autowired OrganizationRepository organizations;

    private Certificate draft() {
        Organization organization = organizations.saveAndFlush(new Organization("Local", UUID.randomUUID() + "@example.com"));
        String publicId;
        do {
            publicId = "CERT-2099-%06d".formatted(ThreadLocalRandom.current().nextInt(1, 1_000_000));
        } while (gateway.findCertificate(hashes.contractKey(publicId)).isPresent()
            || certificates.findByCertificateId(publicId).isPresent());
        return certificates.saveAndFlush(new Certificate(publicId, organization, "Recipient",
            "recipient@example.com", "Program", LocalDate.now()));
    }

    @Test void realGatewayIssuesAndRejectsDuplicateOnChain() {
        Certificate certificate = draft();
        var result = issuance.issue(certificate.getId(), certificate.getOrganization().getId());
        assertEquals(CertificateLifecycle.ISSUED, result.lifecycle());
        assertTrue(gateway.findCertificate(hashes.contractKey(certificate.getCertificateId())).isPresent());
        assertEquals(result.transactionHash(), issuance.issue(certificate.getId(),
            certificate.getOrganization().getId()).transactionHash());

        String key = hashes.contractKey(certificate.getCertificateId());
        try {
            String duplicateHash = gateway.submitIssue(key, "0x" + result.certificateHash(), 0);
            assertFalse(gateway.findReceipt(duplicateHash).orElseThrow().success());
        } catch (com.certchain.blockchain.BlockchainUnavailableException rejectedBeforeMining) {
            assertEquals(CertificateLifecycle.ISSUED,
                certificates.findById(certificate.getId()).orElseThrow().getLifecycle());
        }
    }

    @Test void unknownReceiptStaysPendingAndMinedReceiptReconciles() throws Exception {
        assertTrue(gateway.findReceipt("0x" + "f".repeat(64)).isEmpty());
        Certificate certificate = draft();
        rpc("evm_setAutomine", "[false]");
        try {
            var pending = issuance.issue(certificate.getId(), certificate.getOrganization().getId());
            assertEquals(CertificateLifecycle.ISSUING, pending.lifecycle());
            assertNotNull(pending.transactionHash());
            rpc("evm_mine", "[]");
        } finally {
            rpc("evm_setAutomine", "[true]");
        }
        var confirmed = issuance.reconcile(certificate.getId(), certificate.getOrganization().getId());
        assertEquals(CertificateLifecycle.ISSUED, confirmed.lifecycle());
    }

    @Test void eventSearchRecoversLostHashAndValidatorRejectsWrongEvent() {
        Certificate certificate = draft();
        var prepared = journal.begin(certificate.getId(), certificate.getOrganization().getId(), gateway.identity()).snapshot();
        String hash = gateway.submitIssue(prepared.certificateKey(), "0x" + prepared.certificateHash(), 0);
        var receipt = gateway.findReceipt(hash).orElseThrow();
        var event = receipt.events().getFirst();
        var invalidEvent = new CertificateRegistryGateway.IssueEvent(event.name(), "0x" + "e".repeat(64),
            event.certificateHash(), event.issuer(), event.issuedAt(), event.expiresAt(), event.address());
        var invalidReceipt = new CertificateRegistryGateway.IssueReceipt(hash, true, receipt.chainId(),
            receipt.to(), receipt.blockNumber(), receipt.blockTimestamp(), receipt.confirmations(), List.of(invalidEvent));
        assertThrows(com.certchain.blockchain.InvalidBlockchainReceiptException.class, () ->
            validator.isConfirmed(invalidReceipt, gateway.identity(), hash, prepared.certificateKey(),
                "0x" + prepared.certificateHash(), 0, 1));
        assertEquals(CertificateLifecycle.ISSUED,
            issuance.reconcile(certificate.getId(), certificate.getOrganization().getId()).lifecycle());
    }

    @Test void wrongChainConfigurationFailsStartupValidation() {
        BlockchainSettings wrong = new BlockchainSettings("local", 1, required("CERTCHAIN_LOCAL_RPC"),
            required("CERTCHAIN_LOCAL_CONTRACT"), required("CERTCHAIN_LOCAL_ISSUER_KEY"), 1, 0,
            Duration.ofSeconds(1));
        assertThrows(IllegalStateException.class, () -> new Web3jCertificateRegistryGateway(wrong));
    }

    private static void rpc(String method, String params) throws Exception {
        String body = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"" + method
            + "\",\"params\":" + params + "}";
        HttpRequest request = HttpRequest.newBuilder(URI.create(required("CERTCHAIN_LOCAL_RPC")))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> response = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
            .build().send(request,
            HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 || response.body().contains("\"error\"")) {
            throw new IllegalStateException("Local Hardhat RPC command failed: "
                + response.statusCode() + " " + response.body());
        }
    }
}
