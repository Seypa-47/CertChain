package com.certchain;

import com.certchain.blockchain.BlockchainUnavailableException;
import com.certchain.blockchain.CertificateRegistryGateway;
import com.certchain.certificate.*;
import com.certchain.organization.Organization;
import com.certchain.transaction.*;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PublicVerificationTests {
    private static final Instant ISSUED = Instant.parse("2026-09-01T12:00:00Z");
    private static final CertificateRegistryGateway.ChainIdentity CHAIN =
        new CertificateRegistryGateway.ChainIdentity("sepolia", 11155111L,
            "0x" + "a".repeat(40), "0x" + "b".repeat(40));
    private final CertificateRepository certificates = mock(CertificateRepository.class);
    private final BlockchainTransactionRepository transactions = mock(BlockchainTransactionRepository.class);
    private final CertificateRegistryGateway gateway = mock(CertificateRegistryGateway.class);
    private final CertificateHashService hashes = new CertificateHashService();
    private PublicVerificationController verifier;

    @BeforeEach void setup() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-29T00:00:00Z"), ZoneOffset.UTC);
        verifier = new PublicVerificationController(certificates, transactions, hashes, gateway,
            new CertificateStatusService(clock), new PublicVerificationRateLimiter(clock));
        when(gateway.identity()).thenReturn(CHAIN);
    }

    private Certificate issued(LocalDate expiry) {
        Organization organization = new Organization("Public issuer", "private@example.com");
        ReflectionTestUtils.setField(organization, "id", UUID.randomUUID());
        Certificate certificate = new Certificate("CERT-2026-000001", organization, "Recipient",
            "private@example.com", "Course", LocalDate.of(2026, 9, 1));
        ReflectionTestUtils.setField(certificate, "id", UUID.randomUUID());
        certificate.setExpiryDate(expiry);
        certificate.beginIssuance(hashes.hash(certificate), CertificateHashService.VERSION);
        certificate.markIssued(ISSUED);
        when(certificates.findByCertificateId(certificate.getCertificateId())).thenReturn(Optional.of(certificate));
        BlockchainTransaction issue = journal(certificate, BlockchainTransactionType.ISSUE, CHAIN.chainId());
        when(transactions.findFirstByCertificateIdAndTransactionTypeOrderByCreatedAtDesc(
            certificate.getId(), BlockchainTransactionType.ISSUE)).thenReturn(Optional.of(issue));
        long expiresAt = expiry == null ? 0
            : expiry.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond() - 1;
        when(gateway.findCertificate(hashes.contractKey(certificate.getCertificateId())))
            .thenReturn(Optional.of(new CertificateRegistryGateway.OnChainCertificate(
                "0x" + certificate.getCertificateHash(), ISSUED.getEpochSecond(), expiresAt,
                false, CHAIN.issuerAddress())));
        return certificate;
    }

    private static BlockchainTransaction journal(Certificate certificate, BlockchainTransactionType type, long chainId) {
        BlockchainTransaction tx = new BlockchainTransaction(certificate, type, CHAIN.network(),
            chainId, CHAIN.contractAddress());
        tx.submitted("0x" + (type == BlockchainTransactionType.ISSUE ? "1" : "2").repeat(64), ISSUED);
        tx.confirmed(123, ISSUED, ISSUED);
        return tx;
    }

    @Test void verifiesValidAndExpiredAtExactUtcBoundary() {
        issued(null);
        var valid = verifier.verify("CERT-2026-000001");
        assertEquals(CertificateStatus.VALID, valid.status());
        assertTrue(valid.blockchainVerified());
        assertEquals(PublicVerificationController.ProofResult.VERIFIED, valid.proofResult());
        assertTrue(valid.explorerUrl().startsWith("https://sepolia.etherscan.io/tx/"));
        assertEquals(123, valid.blockNumber());
        issued(LocalDate.of(2026, 9, 28));
        assertEquals(CertificateStatus.EXPIRED, verifier.verify("CERT-2026-000001").status());
    }

    @Test void revokedWinsOverExpiredAndShowsConfirmedRevocationTransaction() {
        Certificate certificate = issued(LocalDate.of(2026, 9, 28));
        certificate.setRevocationReason("private");
        certificate.markRevoked(Instant.parse("2026-09-28T23:00:00Z"));
        var revoke = journal(certificate, BlockchainTransactionType.REVOKE, CHAIN.chainId());
        when(transactions.findFirstByCertificateIdAndTransactionTypeOrderByCreatedAtDesc(
            certificate.getId(), BlockchainTransactionType.REVOKE)).thenReturn(Optional.of(revoke));
        when(gateway.findCertificate(hashes.contractKey(certificate.getCertificateId())))
            .thenReturn(Optional.of(new CertificateRegistryGateway.OnChainCertificate(
                "0x" + certificate.getCertificateHash(), ISSUED.getEpochSecond(),
                LocalDate.of(2026, 9, 29).atStartOfDay(ZoneOffset.UTC).toEpochSecond() - 1,
                true, CHAIN.issuerAddress())));
        var result = verifier.verify(certificate.getCertificateId());
        assertEquals(CertificateStatus.REVOKED, result.status());
        assertEquals(revoke.getTransactionHash(), result.transactionHash());
        assertTrue(result.blockchainVerified());
    }

    @Test void malformedUnknownAndDraftRemainIndistinguishable() {
        assertEquals(404, assertThrows(ResponseStatusException.class,
            () -> verifier.verify("bad-id")).getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class,
            () -> verifier.verify("CERT-2026-000001")).getStatusCode().value());
        Organization org = new Organization("Org", "org@example.com");
        Certificate draft = new Certificate("CERT-2026-000001", org, "Name",
            "email@example.com", "Course", LocalDate.of(2026, 9, 1));
        when(certificates.findByCertificateId(draft.getCertificateId())).thenReturn(Optional.of(draft));
        assertEquals(404, assertThrows(ResponseStatusException.class,
            () -> verifier.verify(draft.getCertificateId())).getStatusCode().value());
    }

    @Test void alteredDataWrongProofWrongChainAndUnavailableNeverReturnValid() {
        Certificate certificate = issued(null);
        String key = hashes.contractKey(certificate.getCertificateId());
        ReflectionTestUtils.setField(certificate, "recipientName", "Tampered");
        assertFailed(verifier.verify(certificate.getCertificateId()),
            PublicVerificationController.ProofResult.PROOF_MISMATCH);
        ReflectionTestUtils.setField(certificate, "recipientName", "Recipient");
        when(gateway.findCertificate(key)).thenReturn(Optional.of(
            new CertificateRegistryGateway.OnChainCertificate("0x" + "f".repeat(64),
                ISSUED.getEpochSecond(), 0, false, CHAIN.issuerAddress())));
        assertFailed(verifier.verify(certificate.getCertificateId()),
            PublicVerificationController.ProofResult.PROOF_MISMATCH);
        when(gateway.findCertificate(key)).thenThrow(new BlockchainUnavailableException("offline"));
        assertFailed(verifier.verify(certificate.getCertificateId()),
            PublicVerificationController.ProofResult.VERIFICATION_UNAVAILABLE);
        doReturn(Optional.of(
            new CertificateRegistryGateway.OnChainCertificate("0x" + certificate.getCertificateHash(),
                ISSUED.getEpochSecond(), 0, false, CHAIN.issuerAddress())))
            .when(gateway).findCertificate(key);
        when(transactions.findFirstByCertificateIdAndTransactionTypeOrderByCreatedAtDesc(
            certificate.getId(), BlockchainTransactionType.ISSUE))
            .thenReturn(Optional.of(journal(certificate, BlockchainTransactionType.ISSUE, 1L)));
        assertFailed(verifier.verify(certificate.getCertificateId()),
            PublicVerificationController.ProofResult.PROOF_MISMATCH);
    }

    private static void assertFailed(PublicVerificationController.PublicVerificationResponse result,
                                     PublicVerificationController.ProofResult expected) {
        assertEquals(expected, result.proofResult());
        assertFalse(result.blockchainVerified());
        assertNull(result.status());
        assertNull(result.transactionHash());
    }
}
