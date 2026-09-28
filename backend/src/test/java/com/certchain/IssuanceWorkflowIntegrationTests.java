package com.certchain;

import com.certchain.blockchain.CertificateRegistryGateway;
import com.certchain.certificate.Certificate;
import com.certchain.certificate.CertificateHashService;
import com.certchain.certificate.CertificateIdGenerator;
import com.certchain.certificate.CertificateLifecycle;
import com.certchain.certificate.CertificateRepository;
import com.certchain.certificate.IssuancePersistence;
import com.certchain.certificate.IssuanceService;
import com.certchain.organization.Organization;
import com.certchain.organization.OrganizationRepository;
import com.certchain.transaction.BlockchainTransactionRepository;
import com.certchain.transaction.BlockchainTransactionStatus;
import com.certchain.transaction.BlockchainTransactionType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "app.auth.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
    "app.blockchain.receipt-timeout=PT0.02S"
})
@Testcontainers(disabledWithoutDocker = true)
@Import(IssuanceWorkflowIntegrationTests.GatewayConfig.class)
class IssuanceWorkflowIntegrationTests {
    @Container @ServiceConnection
    static final PostgreSQLContainer postgres =
        new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));

    @TestConfiguration
    static class GatewayConfig {
        @Bean @Primary FakeGateway fakeGateway() { return new FakeGateway(); }
    }

    static class FakeGateway implements CertificateRegistryGateway {
        enum Mode { SUCCESS, REVERT, TIMEOUT, INVALID_EVENT, WRONG_CHAIN, RPC_ERROR }
        final ChainIdentity chain = new ChainIdentity("local", 31337,
            "0x" + "a".repeat(40), "0x" + "b".repeat(40));
        static final AtomicInteger uniqueHash = new AtomicInteger();
        final AtomicInteger count = new AtomicInteger();
        final Map<String, IssueReceipt> receipts = new HashMap<>();
        final Map<String, String> byKey = new HashMap<>();
        final Map<String, OnChainCertificate> records = new HashMap<>();
        Mode mode = Mode.SUCCESS;

        void reset() { count.set(0); receipts.clear(); byKey.clear(); records.clear(); mode = Mode.SUCCESS; }
        @Override public ChainIdentity identity() { return chain; }
        @Override public Optional<OnChainCertificate> findCertificate(String key) {
            return Optional.ofNullable(records.get(key));
        }
        @Override public String submitIssue(String key, String hash, long expiry) {
            if (mode == Mode.RPC_ERROR) throw new com.certchain.blockchain.BlockchainUnavailableException("RPC down");
            count.incrementAndGet();
            String txHash = "0x" + "%064x".formatted(uniqueHash.incrementAndGet());
            byKey.put(key, txHash);
            if (mode != Mode.TIMEOUT) complete(txHash, key, hash, expiry, mode);
            return txHash;
        }
        void complete(String txHash, String key, String hash, long expiry, Mode outcome) {
            Instant blockTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
            boolean success = outcome != Mode.REVERT;
            if (success) records.put(key, new OnChainCertificate(hash, blockTime.getEpochSecond(),
                expiry, false, chain.issuerAddress()));
            IssueEvent event = new IssueEvent("CertificateIssued",
                outcome == Mode.INVALID_EVENT ? "0x" + "f".repeat(64) : key,
                hash, chain.issuerAddress(), blockTime.getEpochSecond(), expiry, chain.contractAddress());
            receipts.put(txHash, new IssueReceipt(txHash, success,
                outcome == Mode.WRONG_CHAIN ? 1 : chain.chainId(), chain.contractAddress(),
                15, blockTime, 2, success ? List.of(event) : List.of()));
        }
        @Override public Optional<IssueReceipt> findReceipt(String hash) {
            return Optional.ofNullable(receipts.get(hash));
        }
        @Override public Optional<IssueReceipt> findIssueByKey(String key) {
            return Optional.ofNullable(byKey.get(key)).flatMap(this::findReceipt);
        }
        @Override public String submitRevoke(String key) { throw new UnsupportedOperationException(); }
        @Override public Optional<RevokeReceipt> findRevokeReceipt(String hash) { return Optional.empty(); }
        @Override public Optional<RevokeReceipt> findRevokeByKey(String key) { return Optional.empty(); }
    }

    @Autowired FakeGateway chain;
    @Autowired IssuanceService service;
    @Autowired IssuancePersistence journal;
    @Autowired CertificateHashService hashes;
    @Autowired CertificateRepository certificates;
    @Autowired OrganizationRepository organizations;
    @Autowired CertificateIdGenerator ids;
    @Autowired BlockchainTransactionRepository transactions;

    @BeforeEach void reset() { chain.reset(); }

    private Certificate draft() {
        Organization org = organizations.saveAndFlush(new Organization("Issuer", UUID.randomUUID() + "@example.com"));
        return certificates.saveAndFlush(new Certificate(ids.nextId(), org, "Recipient", "recipient@example.com",
            "Program", LocalDate.now()));
    }

    @Test void successCommitsJournalAndFreezesProofFields() {
        Certificate certificate = draft();
        var result = service.issue(certificate.getId(), certificate.getOrganization().getId());
        assertEquals(CertificateLifecycle.ISSUED, result.lifecycle());
        assertEquals(BlockchainTransactionStatus.CONFIRMED, result.transactionStatus());
        assertEquals(1, chain.count.get());
        assertNotNull(result.blockNumber());
        assertNotNull(result.blockTimestamp());
        var saved = certificates.findById(certificate.getId()).orElseThrow();
        assertEquals(CertificateHashService.VERSION, saved.getCanonicalizationVersion());
        assertEquals(hashes.hash(saved), saved.getCertificateHash());
        assertThrows(IllegalStateException.class, () -> saved.setRecipientName("Changed"));
        assertThrows(IllegalStateException.class, () -> saved.setProgramName("Changed"));
        assertThrows(IllegalStateException.class, () -> saved.setIssueDate(LocalDate.now().plusDays(1)));
        assertThrows(IllegalStateException.class, () -> saved.setExpiryDate(LocalDate.now().plusDays(1)));
        var tx = transactions.findFirstByCertificateIdAndTransactionTypeOrderByCreatedAtDesc(
            certificate.getId(), BlockchainTransactionType.ISSUE).orElseThrow();
        assertEquals(result.transactionHash(), tx.getTransactionHash());
        assertNotNull(tx.getSubmittedAt());
        assertNotNull(tx.getConfirmedAt());
        assertEquals(15L, tx.getBlockNumber());
    }

    @Test void revertFailsWithoutIssuingAndCanRetry() {
        Certificate certificate = draft();
        chain.mode = FakeGateway.Mode.REVERT;
        var first = service.issue(certificate.getId(), certificate.getOrganization().getId());
        assertEquals(CertificateLifecycle.ISSUE_FAILED, first.lifecycle());
        assertEquals(BlockchainTransactionStatus.FAILED, first.transactionStatus());
        chain.mode = FakeGateway.Mode.SUCCESS;
        var retried = service.issue(certificate.getId(), certificate.getOrganization().getId());
        assertEquals(CertificateLifecycle.ISSUED, retried.lifecycle());
        assertEquals(2, chain.count.get());
    }

    @Test void timeoutRemainsPendingThenReconcilesWithoutResubmission() {
        Certificate certificate = draft();
        chain.mode = FakeGateway.Mode.TIMEOUT;
        var pending = service.issue(certificate.getId(), certificate.getOrganization().getId());
        assertEquals(CertificateLifecycle.ISSUING, pending.lifecycle());
        assertEquals(BlockchainTransactionStatus.SUBMITTED, pending.transactionStatus());
        assertEquals(1, chain.count.get());
        var repeat = service.issue(certificate.getId(), certificate.getOrganization().getId());
        assertEquals(CertificateLifecycle.ISSUING, repeat.lifecycle());
        assertEquals(1, chain.count.get());
        chain.complete(pending.transactionHash(), hashes.contractKey(certificate.getCertificateId()),
            "0x" + pending.certificateHash(), 0, FakeGateway.Mode.SUCCESS);
        var confirmed = service.reconcile(certificate.getId(), certificate.getOrganization().getId());
        assertEquals(CertificateLifecycle.ISSUED, confirmed.lifecycle());
        assertEquals(1, chain.count.get());
        assertEquals(CertificateLifecycle.ISSUED,
            service.issue(certificate.getId(), certificate.getOrganization().getId()).lifecycle());
        assertEquals(1, chain.count.get());
    }

    @Test void invalidEventAndWrongChainNeverIssue() {
        for (FakeGateway.Mode mode : List.of(FakeGateway.Mode.INVALID_EVENT, FakeGateway.Mode.WRONG_CHAIN)) {
            chain.reset();
            Certificate certificate = draft();
            chain.mode = mode;
            var result = service.issue(certificate.getId(), certificate.getOrganization().getId());
            assertEquals(CertificateLifecycle.ISSUE_FAILED, result.lifecycle());
            assertNotNull(result.failureReason());
            assertNull(certificates.findById(certificate.getId()).orElseThrow().getIssuedAt());
            assertEquals(CertificateLifecycle.ISSUE_FAILED,
                service.issue(certificate.getId(), certificate.getOrganization().getId()).lifecycle());
            assertEquals(1, chain.count.get());
        }
    }

    @Test void uncertainRpcSubmissionStaysPendingUntilProofCanBeRecovered() {
        Certificate certificate = draft();
        chain.mode = FakeGateway.Mode.RPC_ERROR;
        var pending = service.issue(certificate.getId(), certificate.getOrganization().getId());
        assertEquals(CertificateLifecycle.ISSUING, pending.lifecycle());
        assertEquals(BlockchainTransactionStatus.CREATED, pending.transactionStatus());
        chain.mode = FakeGateway.Mode.SUCCESS;
        assertEquals(CertificateLifecycle.ISSUING,
            service.issue(certificate.getId(), certificate.getOrganization().getId()).lifecycle());
        assertEquals(0, chain.count.get());
        chain.submitIssue(hashes.contractKey(certificate.getCertificateId()),
            "0x" + pending.certificateHash(), 0);
        var success = service.reconcile(certificate.getId(), certificate.getOrganization().getId());
        assertEquals(CertificateLifecycle.ISSUED, success.lifecycle());
        assertEquals(1, chain.count.get());
    }

    @Test void recoversCrashAfterSubmissionBeforeHashPersistence() {
        Certificate certificate = draft();
        var prepared = journal.begin(certificate.getId(), certificate.getOrganization().getId(), chain.identity()).snapshot();
        String txHash = chain.submitIssue(prepared.certificateKey(), "0x" + prepared.certificateHash(),
            prepared.expiresAt());
        assertNull(journal.snapshot(certificate.getId(), certificate.getOrganization().getId()).transactionHash());
        var result = service.reconcile(certificate.getId(), certificate.getOrganization().getId());
        assertEquals(CertificateLifecycle.ISSUED, result.lifecycle());
        assertEquals(txHash, result.transactionHash());
        assertEquals(1, chain.count.get());
    }

    @Test void concurrentIssueRequestsSubmitOnlyOnce() throws Exception {
        Certificate certificate = draft();
        UUID certificateId = certificate.getId();
        UUID organizationId = certificate.getOrganization().getId();
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                start.await();
                return service.issue(certificateId, organizationId);
            });
            var second = executor.submit(() -> {
                start.await();
                return service.issue(certificateId, organizationId);
            });
            start.countDown();
            first.get(15, TimeUnit.SECONDS);
            second.get(15, TimeUnit.SECONDS);
        }
        assertEquals(1, chain.count.get());
        assertEquals(CertificateLifecycle.ISSUED,
            certificates.findById(certificateId).orElseThrow().getLifecycle());
    }
}
