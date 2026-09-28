package com.certchain.certificate;

import com.certchain.blockchain.CertificateRegistryGateway;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/public/certificates")
public class PublicVerificationController {
    private final CertificateRepository certificates;
    private final CertificateHashService hashes;
    private final CertificateRegistryGateway gateway;
    private final CertificateStatusService statuses;

    public PublicVerificationController(CertificateRepository certificates, CertificateHashService hashes,
                                        CertificateRegistryGateway gateway, CertificateStatusService statuses) {
        this.certificates = certificates;
        this.hashes = hashes;
        this.gateway = gateway;
        this.statuses = statuses;
    }

    @GetMapping("/{certificateId}")
    public PublicVerificationResponse verify(@PathVariable String certificateId) {
        Certificate certificate = certificates.findByCertificateId(certificateId)
            .filter(value -> value.getLifecycle() == CertificateLifecycle.ISSUED)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificate not found"));
        String localHash = hashes.hash(certificate);
        var record = gateway.findCertificate(hashes.contractKey(certificate.getCertificateId()))
            .orElseThrow(() -> new CertificateConflictException("PROOF_MISMATCH", "On-chain proof is missing"));
        if (!localHash.equalsIgnoreCase(certificate.getCertificateHash())
            || !("0x" + localHash).equalsIgnoreCase(record.certificateHash())
            || certificate.getIssuedAt() == null
            || record.issuedAt() != certificate.getIssuedAt().getEpochSecond()
            || record.expiresAt() != (certificate.getExpiryDate() == null ? 0
                : certificate.getExpiryDate().plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond() - 1)
            || (certificate.getRevokedAt() != null) != record.revoked()) {
            throw new CertificateConflictException("PROOF_MISMATCH", "Database and on-chain proof differ");
        }
        return new PublicVerificationResponse(certificate.getCertificateId(),
            certificate.getRecipientName(), certificate.getProgramName(), certificate.getOrganization().getName(),
            certificate.getIssueDate(), certificate.getExpiryDate(), statuses.status(certificate),
            true, certificate.getIssuedAt(), certificate.getRevokedAt(),
            gateway.identity().network(), gateway.identity().chainId(), gateway.identity().contractAddress());
    }

    public record PublicVerificationResponse(String certificateId, String recipientName,
        String programName, String organizationName, LocalDate issueDate, LocalDate expiryDate,
        CertificateStatus status, boolean blockchainVerified, Instant issuedAt, Instant revokedAt,
        String network, long chainId, String contractAddress) {}
}
