package com.certchain.email;

import com.certchain.certificate.Certificate;
import com.certchain.common.domain.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_delivery")
public class EmailDelivery extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "certificate_id", nullable = false)
    private Certificate certificate;
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type", nullable = false, length = 32)
    private EmailDeliveryType deliveryType;
    @Column(name = "recipient_email", nullable = false, length = 320)
    private String recipientEmail;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    private EmailDeliveryStatus status;
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;
    @Column(columnDefinition = "text")
    private String failureReason;
    @Column(name = "sent_at", nullable = true)
    private Instant sentAt;

    protected EmailDelivery() {}
    public EmailDelivery(Certificate certificate, EmailDeliveryType deliveryType, String recipientEmail) {
        this.certificate = java.util.Objects.requireNonNull(certificate);
        this.deliveryType = java.util.Objects.requireNonNull(deliveryType);
        this.recipientEmail = java.util.Objects.requireNonNull(recipientEmail);
        this.status = EmailDeliveryStatus.PENDING;
        this.attemptCount = 0;
    }

    public Certificate getCertificate() { return certificate; }
    public EmailDeliveryType getDeliveryType() { return deliveryType; }
    public String getRecipientEmail() { return recipientEmail; }
    public EmailDeliveryStatus getStatus() { return status; }
    public int getAttemptCount() { return attemptCount; }
    public String getFailureReason() { return failureReason; }
    public Instant getSentAt() { return sentAt; }
    public UUID getId() { return id; }
    public void beginAttempt(String currentRecipient) {
        recipientEmail = java.util.Objects.requireNonNull(currentRecipient);
        attemptCount++;
        status = EmailDeliveryStatus.PENDING;
        failureReason = null;
    }
    public void markSent(Instant when) {
        status = EmailDeliveryStatus.SENT;
        sentAt = java.util.Objects.requireNonNull(when);
        failureReason = null;
    }
    public void markFailed(String safeReason) {
        status = EmailDeliveryStatus.FAILED;
        failureReason = java.util.Objects.requireNonNull(safeReason);
    }
}
