package com.certchain.certificate;

import com.certchain.blockchain.BlockchainUnavailableException;
import com.certchain.blockchain.CertificateRegistryGateway;
import com.certchain.transaction.BlockchainTransaction;
import com.certchain.transaction.BlockchainTransactionRepository;
import com.certchain.transaction.BlockchainTransactionStatus;
import com.certchain.transaction.BlockchainTransactionType;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/public/certificates")
public class PublicVerificationController {
    private static final Pattern PUBLIC_ID = Pattern.compile("CERT-[0-9]{4}-(?!000000)[0-9]{6}");
    private static final Pattern TX_HASH = Pattern.compile("0x[0-9a-fA-F]{64}");
    private final CertificateRepository certificates;
    private final BlockchainTransactionRepository transactions;
    private final CertificateHashService hashes;
    private final CertificateRegistryGateway gateway;
    private final CertificateStatusService statuses;
    private final PublicVerificationRateLimiter rateLimiter;

    public PublicVerificationController(CertificateRepository certificates,
                                        BlockchainTransactionRepository transactions,
                                        CertificateHashService hashes, CertificateRegistryGateway gateway,
                                        CertificateStatusService statuses, PublicVerificationRateLimiter rateLimiter) {
        this.certificates = certificates;
        this.transactions = transactions;
        this.hashes = hashes;
        this.gateway = gateway;
        this.statuses = statuses;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/{certificateId}")
    @Transactional(readOnly = true)
    public PublicVerificationResponse verify(@PathVariable String certificateId, HttpServletRequest request) {
        rateLimiter.check(request.getRemoteAddr());
        return verify(certificateId);
    }

    @Transactional(readOnly = true)
    public PublicVerificationResponse verify(String certificateId) {
        if (!PUBLIC_ID.matcher(certificateId).matches()) throw notFound();
        Certificate certificate = certificates.findByCertificateId(certificateId)
            .filter(value -> value.getLifecycle() == CertificateLifecycle.ISSUED)
            .orElseThrow(PublicVerificationController::notFound);
        String localHash = hashes.hash(certificate);
        BlockchainTransaction issue = transactions
            .findFirstByCertificateIdAndTransactionTypeOrderByCreatedAtDesc(
                certificate.getId(), BlockchainTransactionType.ISSUE).orElse(null);
        var identity = safeIdentity();
        if (identity == null) return response(certificate, ProofResult.VERIFICATION_UNAVAILABLE, null, null);
        if (certificate.getCertificateHash() == null || !localHash.equalsIgnoreCase(certificate.getCertificateHash())
            || !CertificateHashService.VERSION.equals(certificate.getCanonicalizationVersion())
            || !validJournal(issue, identity)) {
            return response(certificate, ProofResult.PROOF_MISMATCH, null, null);
        }
        BlockchainTransaction revoke = null;
        if (certificate.getRevokedAt() != null) {
            revoke = transactions.findFirstByCertificateIdAndTransactionTypeOrderByCreatedAtDesc(
                certificate.getId(), BlockchainTransactionType.REVOKE).orElse(null);
            if (!validJournal(revoke, identity)) {
                return response(certificate, ProofResult.PROOF_MISMATCH, null, null);
            }
        }
        try {
            var record = gateway.findCertificate(hashes.contractKey(certificateId));
            long expiresAt = certificate.getExpiryDate() == null ? 0
                : certificate.getExpiryDate().plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond() - 1;
            if (record.isEmpty() || certificate.getIssuedAt() == null
                || !("0x" + localHash).equalsIgnoreCase(record.orElseThrow().certificateHash())
                || record.orElseThrow().issuedAt() != certificate.getIssuedAt().getEpochSecond()
                || record.orElseThrow().expiresAt() != expiresAt
                || record.orElseThrow().revoked() != (certificate.getRevokedAt() != null)
                || !identity.issuerAddress().equalsIgnoreCase(record.orElseThrow().issuer())) {
                return response(certificate, ProofResult.PROOF_MISMATCH, null, null);
            }
            return response(certificate, ProofResult.VERIFIED, issue, revoke);
        } catch (BlockchainUnavailableException unavailable) {
            return response(certificate, ProofResult.VERIFICATION_UNAVAILABLE, null, null);
        }
    }

    private CertificateRegistryGateway.ChainIdentity safeIdentity() {
        try { return gateway.identity(); }
        catch (BlockchainUnavailableException unavailable) { return null; }
    }

    private static boolean validJournal(BlockchainTransaction tx, CertificateRegistryGateway.ChainIdentity chain) {
        return tx != null && tx.getStatus() == BlockchainTransactionStatus.CONFIRMED
            && tx.getTransactionHash() != null && TX_HASH.matcher(tx.getTransactionHash()).matches()
            && tx.getBlockNumber() != null && tx.getBlockNumber() > 0 && tx.getBlockTimestamp() != null
            && tx.getChainId() == chain.chainId() && tx.getNetwork().equals(chain.network())
            && tx.getContractAddress().equalsIgnoreCase(chain.contractAddress());
    }

    private PublicVerificationResponse response(Certificate certificate, ProofResult proof,
                                                BlockchainTransaction issue, BlockchainTransaction revoke) {
        BlockchainTransaction latest = revoke == null ? issue : revoke;
        return new PublicVerificationResponse(certificate.getCertificateId(), certificate.getRecipientName(),
            certificate.getProgramName(), certificate.getOrganization().getName(), certificate.getIssueDate(),
            certificate.getExpiryDate(), proof == ProofResult.VERIFIED ? statuses.status(certificate) : null,
            proof, proof == ProofResult.VERIFIED, proof == ProofResult.VERIFIED ? certificate.getIssuedAt() : null,
            proof == ProofResult.VERIFIED ? certificate.getRevokedAt() : null,
            latest == null ? null : latest.getNetwork(), latest == null ? null : latest.getChainId(),
            latest == null ? null : latest.getContractAddress(), latest == null ? null : latest.getTransactionHash(),
            latest == null ? null : latest.getBlockNumber(), latest == null ? null : latest.getBlockTimestamp(),
            latest == null ? null : explorer(latest));
    }

    private static String explorer(BlockchainTransaction tx) {
        if (!TX_HASH.matcher(tx.getTransactionHash()).matches()) return null;
        String base = switch (tx.getNetwork()) {
            case "sepolia" -> tx.getChainId() == 11155111 ? "https://sepolia.etherscan.io/tx/" : null;
            case "mainnet" -> tx.getChainId() == 1 ? "https://etherscan.io/tx/" : null;
            default -> null;
        };
        return base == null ? null : base + tx.getTransactionHash();
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificate not found");
    }

    public enum ProofResult { VERIFIED, PROOF_MISMATCH, VERIFICATION_UNAVAILABLE }

    public record PublicVerificationResponse(String certificateId, String recipientName,
        String programName, String organizationName, LocalDate issueDate, LocalDate expiryDate,
        CertificateStatus status, ProofResult proofResult, boolean blockchainVerified,
        Instant issuedAt, Instant revokedAt, String network, Long chainId, String contractAddress,
        String transactionHash, Long blockNumber, Instant blockTimestamp, String explorerUrl) {}
}
