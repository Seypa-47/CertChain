package com.certchain.certificate;

import com.certchain.common.domain.AuditableEntity;
import com.certchain.organization.Organization;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "certificate")
public class Certificate extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "certificate_id", nullable = false, length = 16)
    private String certificateId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @Column(name = "recipient_name", nullable = false, length = 200)
    private String recipientName;
    @Column(name = "recipient_email", nullable = false, length = 320)
    private String recipientEmail;
    @Column(name = "program_name", nullable = false, length = 300)
    private String programName;
    @Column(columnDefinition = "text")
    private String description;
    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;
    @Column(name = "expiry_date", nullable = true)
    private LocalDate expiryDate;
    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle", nullable = false, length = 20)
    private CertificateLifecycle lifecycle;
    @Column(name = "canonicalization_version", nullable = true, length = 16)
    private String canonicalizationVersion;
    @Column(name = "certificate_hash", nullable = true, length = 64)
    private String certificateHash;
    @Column(name = "pdf_storage_key", nullable = true, length = 1024)
    private String pdfStorageKey;
    @Column(name = "issued_at", nullable = true)
    private Instant issuedAt;
    @Column(name = "revoked_at", nullable = true)
    private Instant revokedAt;
    @Column(columnDefinition = "text")
    private String revocationReason;
    @Version
    @Column(nullable = false)
    private long version;

    protected Certificate() {}
    public Certificate(String certificateId, Organization organization, String recipientName, String recipientEmail, String programName, LocalDate issueDate) {
        this.certificateId = java.util.Objects.requireNonNull(certificateId);
        this.organization = java.util.Objects.requireNonNull(organization);
        this.recipientName = java.util.Objects.requireNonNull(recipientName);
        this.recipientEmail = java.util.Objects.requireNonNull(recipientEmail);
        this.programName = java.util.Objects.requireNonNull(programName);
        this.issueDate = java.util.Objects.requireNonNull(issueDate);
        this.lifecycle = CertificateLifecycle.DRAFT;
    }

    public String getCertificateId() { return certificateId; }
    public Organization getOrganization() { return organization; }
    public String getRecipientName() { return recipientName; }
    public String getRecipientEmail() { return recipientEmail; }
    public String getProgramName() { return programName; }
    public String getDescription() { return description; }
    public LocalDate getIssueDate() { return issueDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public CertificateLifecycle getLifecycle() { return lifecycle; }
    public String getCanonicalizationVersion() { return canonicalizationVersion; }
    public String getCertificateHash() { return certificateHash; }
    public String getPdfStorageKey() { return pdfStorageKey; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public String getRevocationReason() { return revocationReason; }
    public UUID getId() { return id; }
    public long getVersion() { return version; }
    public void setRecipientName(String value) { requireDraft(); this.recipientName = value; }
    public void setRecipientEmail(String value) { this.recipientEmail = value; }
    public void setProgramName(String value) { requireDraft(); this.programName = value; }
    public void setDescription(String value) { this.description = value; }
    public void setIssueDate(LocalDate value) { requireDraft(); this.issueDate = value; }
    public void setExpiryDate(LocalDate value) { requireDraft(); this.expiryDate = value; }
    public void setPdfStorageKey(String value) { this.pdfStorageKey = value; }
    public void setIssuedAt(Instant value) { this.issuedAt = value; }
    public void setRevokedAt(Instant value) { this.revokedAt = value; }
    public void setRevocationReason(String value) { this.revocationReason = value; }

    public void beginIssuance(String hash, String version) {
        if (lifecycle != CertificateLifecycle.DRAFT && lifecycle != CertificateLifecycle.ISSUE_FAILED) {
            throw new IllegalStateException("Certificate cannot start issuance");
        }
        if (certificateHash != null && !certificateHash.equals(hash)) {
            throw new IllegalStateException("Frozen certificate proof has changed");
        }
        if (canonicalizationVersion != null && !canonicalizationVersion.equals(version)) {
            throw new IllegalStateException("Frozen canonicalization version has changed");
        }
        certificateHash = java.util.Objects.requireNonNull(hash);
        canonicalizationVersion = java.util.Objects.requireNonNull(version);
        lifecycle = CertificateLifecycle.ISSUING;
    }

    public void markIssued(Instant when) {
        if (lifecycle != CertificateLifecycle.ISSUING) throw new IllegalStateException("Certificate is not issuing");
        issuedAt = java.util.Objects.requireNonNull(when);
        lifecycle = CertificateLifecycle.ISSUED;
    }

    public void markIssueFailed() {
        if (lifecycle != CertificateLifecycle.ISSUING) throw new IllegalStateException("Certificate is not issuing");
        lifecycle = CertificateLifecycle.ISSUE_FAILED;
    }

    private void requireDraft() {
        if (lifecycle != CertificateLifecycle.DRAFT) {
            throw new IllegalStateException("Proof fields are frozen after issuance begins");
        }
    }
}
