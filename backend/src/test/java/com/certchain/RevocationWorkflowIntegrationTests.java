package com.certchain;

import com.certchain.blockchain.CertificateRegistryGateway;
import com.certchain.certificate.*;
import com.certchain.organization.Organization;
import com.certchain.organization.OrganizationRepository;
import com.certchain.transaction.BlockchainTransactionRepository;
import com.certchain.transaction.BlockchainTransaction;
import com.certchain.transaction.BlockchainTransactionStatus;
import com.certchain.transaction.BlockchainTransactionType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "app.auth.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
    "app.blockchain.receipt-timeout=PT0.02S"
})
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureMockMvc
@Import(RevocationWorkflowIntegrationTests.GatewayConfig.class)
class RevocationWorkflowIntegrationTests {
    @Container @ServiceConnection
    static final PostgreSQLContainer postgres =
        new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));

    @TestConfiguration
    static class GatewayConfig {
        @Bean @Primary FakeGateway fakeGateway() { return new FakeGateway(); }
    }

    static class FakeGateway implements CertificateRegistryGateway {
        enum Mode { SUCCESS, REVERT, TIMEOUT, INVALID_EVENT }
        static final AtomicInteger hashes = new AtomicInteger();
        final ChainIdentity identity = new ChainIdentity("local", 31337, "0x" + "a".repeat(40), "0x" + "b".repeat(40));
        final AtomicInteger submissions = new AtomicInteger();
        final Map<String, OnChainCertificate> records = new HashMap<>();
        final Map<String, RevokeReceipt> receipts = new HashMap<>();
        final Map<String, String> byKey = new HashMap<>();
        Mode mode = Mode.SUCCESS;
        boolean unavailable;
        void reset() { submissions.set(0); records.clear(); receipts.clear(); byKey.clear(); mode = Mode.SUCCESS; unavailable = false; }
        @Override public ChainIdentity identity() { return identity; }
        @Override public Optional<OnChainCertificate> findCertificate(String key) {
            if (unavailable) throw new com.certchain.blockchain.BlockchainUnavailableException("RPC unavailable");
            return Optional.ofNullable(records.get(key));
        }
        @Override public String submitIssue(String key, String hash, long expiry) { throw new UnsupportedOperationException(); }
        @Override public Optional<IssueReceipt> findReceipt(String hash) { return Optional.empty(); }
        @Override public Optional<IssueReceipt> findIssueByKey(String key) { return Optional.empty(); }
        @Override public String submitRevoke(String key) {
            submissions.incrementAndGet();
            String hash = "0x" + "%064x".formatted(hashes.incrementAndGet());
            byKey.put(key, hash);
            if (mode != Mode.TIMEOUT) complete(hash, key, mode);
            return hash;
        }
        void complete(String hash, String key, Mode outcome) {
            long second = Instant.now().getEpochSecond();
            if (outcome != Mode.REVERT) {
                var existing = records.get(key);
                records.put(key, new OnChainCertificate(existing.certificateHash(), existing.issuedAt(),
                    existing.expiresAt(), true, existing.issuer()));
            }
            var event = new RevokeEvent("CertificateRevoked",
                outcome == Mode.INVALID_EVENT ? "0x" + "f".repeat(64) : key,
                identity.issuerAddress(), second, identity.contractAddress());
            receipts.put(hash, new RevokeReceipt(hash, outcome != Mode.REVERT, identity.chainId(),
                identity.contractAddress(), 10, Instant.ofEpochSecond(second), 2,
                outcome == Mode.REVERT ? List.of() : List.of(event)));
        }
        @Override public Optional<RevokeReceipt> findRevokeReceipt(String hash) { return Optional.ofNullable(receipts.get(hash)); }
        @Override public Optional<RevokeReceipt> findRevokeByKey(String key) {
            return Optional.ofNullable(byKey.get(key)).flatMap(this::findRevokeReceipt);
        }
    }

    @Autowired FakeGateway chain;
    @Autowired RevocationService service;
    @Autowired RevocationPersistence journal;
    @Autowired CertificateRepository certificates;
    @Autowired OrganizationRepository organizations;
    @Autowired CertificateHashService hashes;
    @Autowired BlockchainTransactionRepository transactions;
    @Autowired PublicVerificationController publicVerification;
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach void reset() { chain.reset(); }

    private Certificate issued(LocalDate expiry) {
        Organization organization = organizations.saveAndFlush(new Organization("Issuer", UUID.randomUUID() + "@example.com"));
        Certificate certificate = new Certificate("CERT-2099-%06d".formatted(FakeGateway.hashes.incrementAndGet()),
            organization, "Recipient", "private@example.com", "Course", LocalDate.of(2026, 1, 1));
        certificate.setExpiryDate(expiry);
        certificate.beginIssuance(hashes.hash(certificate), CertificateHashService.VERSION);
        certificate.markIssued(Instant.parse("2026-01-01T12:00:00Z"));
        certificate = certificates.saveAndFlush(certificate);
        BlockchainTransaction issue = new BlockchainTransaction(certificate, BlockchainTransactionType.ISSUE,
            chain.identity.network(), chain.identity.chainId(), chain.identity.contractAddress());
        issue.submitted("0x" + "%064x".formatted(FakeGateway.hashes.incrementAndGet()),
            certificate.getIssuedAt());
        issue.confirmed(1, certificate.getIssuedAt(), certificate.getIssuedAt());
        transactions.saveAndFlush(issue);
        chain.records.put(hashes.contractKey(certificate.getCertificateId()),
            new CertificateRegistryGateway.OnChainCertificate("0x" + certificate.getCertificateHash(),
                certificate.getIssuedAt().getEpochSecond(), expiry == null ? 0
                    : expiry.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond() - 1,
                false, chain.identity.issuerAddress()));
        return certificate;
    }

    @Test void confirmedRevocationPersistsPrivateReasonAndPublicStatus() throws Exception {
        Certificate certificate = issued(null);
        var result = service.revoke(certificate.getId(), certificate.getOrganization().getId(), "  Private correction  ");
        assertNotNull(result.revokedAt());
        assertEquals(BlockchainTransactionStatus.CONFIRMED, result.transactionStatus());
        assertEquals("Private correction", certificates.findById(certificate.getId()).orElseThrow().getRevocationReason());
        var tx = transactions.findFirstByCertificateIdAndTransactionTypeOrderByCreatedAtDesc(
            certificate.getId(), BlockchainTransactionType.REVOKE).orElseThrow();
        assertNotNull(tx.getConfirmedAt());
        assertEquals(com.certchain.certificate.CertificateStatus.REVOKED,
            publicVerification.verify(certificate.getCertificateId()).status());
        mvc.perform(get("/api/public/certificates/{id}", certificate.getCertificateId()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REVOKED"))
            .andExpect(jsonPath("$.revocationReason").doesNotExist())
            .andExpect(jsonPath("$.recipientEmail").doesNotExist());
        mvc.perform(get("/api/public/certificates/{id}", certificate.getCertificateId()))
            .andExpect(jsonPath("$.proofResult").value("VERIFIED"))
            .andExpect(jsonPath("$.transactionHash").exists())
            .andExpect(jsonPath("$.id").doesNotExist());
        assertEquals(1, chain.submissions.get());
        assertEquals("CERTIFICATE_ALREADY_REVOKED", assertThrows(CertificateConflictException.class,
            () -> service.revoke(certificate.getId(), certificate.getOrganization().getId(), "again")).code());
    }

    @Test void publicVerificationSeparatesMissingMismatchAndUnavailable() throws Exception {
        Certificate issued = issued(null);
        String id = issued.getCertificateId();
        mvc.perform(get("/api/public/certificates/{id}", "bad-id"))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/public/certificates/{id}", "CERT-2099-999999"))
            .andExpect(status().isNotFound());
        Organization organization = issued.getOrganization();
        Certificate draft = certificates.saveAndFlush(new Certificate(
            "CERT-2099-%06d".formatted(FakeGateway.hashes.incrementAndGet()), organization,
            "Private", "private@example.com", "Course", LocalDate.now()));
        mvc.perform(get("/api/public/certificates/{id}", draft.getCertificateId()))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/public/certificates/{id}", id))
            .andExpect(status().isOk()).andExpect(jsonPath("$.proofResult").value("VERIFIED"))
            .andExpect(jsonPath("$.status").value("VALID"));
        chain.records.remove(hashes.contractKey(id));
        mvc.perform(get("/api/public/certificates/{id}", id))
            .andExpect(status().isOk()).andExpect(jsonPath("$.proofResult").value("PROOF_MISMATCH"))
            .andExpect(jsonPath("$.status").isEmpty())
            .andExpect(jsonPath("$.blockchainVerified").value(false));
        chain.unavailable = true;
        mvc.perform(get("/api/public/certificates/{id}", id))
            .andExpect(status().isOk()).andExpect(jsonPath("$.proofResult").value("VERIFICATION_UNAVAILABLE"))
            .andExpect(jsonPath("$.status").isEmpty());
        chain.unavailable = false;
        chain.records.put(hashes.contractKey(id), new CertificateRegistryGateway.OnChainCertificate(
            "0x" + "f".repeat(64), issued.getIssuedAt().getEpochSecond(), 0, false,
            chain.identity.issuerAddress()));
        mvc.perform(get("/api/public/certificates/{id}", id))
            .andExpect(jsonPath("$.proofResult").value("PROOF_MISMATCH"));
        jdbc.update("update certificate set recipient_name = ? where id = ?", "Altered", issued.getId());
        mvc.perform(get("/api/public/certificates/{id}", id))
            .andExpect(jsonPath("$.proofResult").value("PROOF_MISMATCH"));
    }

    @Test void wrongJournalChainFailsProof() throws Exception {
        Certificate certificate = issued(null);
        jdbc.update("update blockchain_transaction set chain_id = ? where certificate_id = ?", 1L, certificate.getId());
        mvc.perform(get("/api/public/certificates/{id}", certificate.getCertificateId()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.proofResult").value("PROOF_MISMATCH"));
    }

    @Test void rejectsDraftAndCrossTenantAndInvalidReason() {
        Certificate certificate = issued(null);
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
            () -> service.revoke(certificate.getId(), UUID.randomUUID(), "reason"));
        assertEquals("REVOCATION_REASON_INVALID", assertThrows(CertificateConflictException.class,
            () -> service.revoke(certificate.getId(), certificate.getOrganization().getId(), " ")).code());
        Organization organization = organizations.saveAndFlush(new Organization("Draft", UUID.randomUUID() + "@example.com"));
        Certificate draft = certificates.saveAndFlush(new Certificate("CERT-2099-%06d".formatted(FakeGateway.hashes.incrementAndGet()),
            organization, "Recipient", "recipient@example.com", "Course", LocalDate.now()));
        assertEquals("CERTIFICATE_NOT_ISSUED", assertThrows(CertificateConflictException.class,
            () -> service.revoke(draft.getId(), organization.getId(), "reason")).code());
        draft.beginIssuance("a".repeat(64), "v1");
        draft.markIssueFailed();
        certificates.saveAndFlush(draft);
        assertEquals("CERTIFICATE_NOT_ISSUED", assertThrows(CertificateConflictException.class,
            () -> service.revoke(draft.getId(), organization.getId(), "reason")).code());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
            () -> service.revoke(UUID.randomUUID(), organization.getId(), "reason"));
    }

    @Test void expiredCertificateCanBeRevoked() {
        Certificate certificate = issued(LocalDate.of(2026, 2, 1));
        assertEquals(com.certchain.certificate.CertificateStatus.EXPIRED,
            publicVerification.verify(certificate.getCertificateId()).status());
        service.revoke(certificate.getId(), certificate.getOrganization().getId(), "Expired proof corrected");
        assertEquals(com.certchain.certificate.CertificateStatus.REVOKED,
            publicVerification.verify(certificate.getCertificateId()).status());
    }

    @Test void timeoutStaysPendingAndReconcilesWithoutDuplicateCall() {
        Certificate certificate = issued(null);
        chain.mode = FakeGateway.Mode.TIMEOUT;
        var pending = service.revoke(certificate.getId(), certificate.getOrganization().getId(), "Private reason");
        assertNull(pending.revokedAt());
        assertEquals(BlockchainTransactionStatus.SUBMITTED, pending.transactionStatus());
        service.revoke(certificate.getId(), certificate.getOrganization().getId(), "Private reason");
        assertEquals(1, chain.submissions.get());
        chain.complete(pending.transactionHash(), hashes.contractKey(certificate.getCertificateId()), FakeGateway.Mode.SUCCESS);
        assertNotNull(service.reconcile(certificate.getId(), certificate.getOrganization().getId()).revokedAt());
        assertEquals(1, chain.submissions.get());
    }

    @Test void revertFailsAndSafeRetrySucceeds() {
        Certificate certificate = issued(null);
        chain.mode = FakeGateway.Mode.REVERT;
        var failed = service.revoke(certificate.getId(), certificate.getOrganization().getId(), "Private reason");
        assertNull(failed.revokedAt());
        assertEquals(BlockchainTransactionStatus.FAILED, failed.transactionStatus());
        chain.mode = FakeGateway.Mode.SUCCESS;
        assertNotNull(service.revoke(certificate.getId(), certificate.getOrganization().getId(), "Private reason").revokedAt());
        assertEquals(2, chain.submissions.get());
    }

    @Test void invalidEventNeverSetsRevokedAt() {
        Certificate certificate = issued(null);
        chain.mode = FakeGateway.Mode.INVALID_EVENT;
        var failed = service.revoke(certificate.getId(), certificate.getOrganization().getId(), "Private reason");
        assertEquals(BlockchainTransactionStatus.FAILED, failed.transactionStatus());
        assertNull(certificates.findById(certificate.getId()).orElseThrow().getRevokedAt());
    }

    @Test void recoversSubmittedTransactionWhoseHashWasNotPersisted() {
        Certificate certificate = issued(null);
        var prepared = journal.begin(certificate.getId(), certificate.getOrganization().getId(),
            "Private reason", chain.identity()).snapshot();
        String hash = chain.submitRevoke(prepared.key());
        assertNull(journal.snapshot(certificate.getId(), certificate.getOrganization().getId()).transactionHash());
        var recovered = service.reconcile(certificate.getId(), certificate.getOrganization().getId());
        assertEquals(hash, recovered.transactionHash());
        assertNotNull(recovered.revokedAt());
        assertEquals(1, chain.submissions.get());
    }
}
