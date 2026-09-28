package com.certchain.email;

import com.certchain.artifact.CertificateArtifactService;
import com.certchain.artifact.CertificateArtifactService.ArtifactState;
import com.certchain.artifact.VerificationUrlFactory;
import com.certchain.certificate.Certificate;
import com.certchain.certificate.CertificateConflictException;
import com.certchain.certificate.CertificateLifecycle;
import com.certchain.certificate.CertificateRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CertificateEmailDeliveryService {
    private static final Logger log = LoggerFactory.getLogger(CertificateEmailDeliveryService.class);
    private static final int MAX_AUTOMATIC_ATTEMPTS = 3;
    private static final int MAX_TOTAL_ATTEMPTS = 5;
    private static final Duration RETRY_DELAY = Duration.ofMinutes(2);
    private final CertificateRepository certificates;
    private final EmailDeliveryRepository deliveries;
    private final CertificateArtifactService artifacts;
    private final VerificationUrlFactory urls;
    private final CertificateEmailService email;
    private final TransactionTemplate transactions;
    private final Clock clock;
    private final boolean enabled;

    public CertificateEmailDeliveryService(CertificateRepository certificates, EmailDeliveryRepository deliveries,
        CertificateArtifactService artifacts, VerificationUrlFactory urls, CertificateEmailService email,
        TransactionTemplate transactions, Clock clock, @Value("${app.mail.enabled:false}") boolean enabled) {
        this.certificates = certificates;
        this.deliveries = deliveries;
        this.artifacts = artifacts;
        this.urls = urls;
        this.email = email;
        this.transactions = transactions;
        this.clock = clock;
        this.enabled = enabled;
    }

    /** Called after artifact creation. A delivery failure is independent of the issued proof. */
    public void deliverAfterArtifact(UUID certificateId, UUID organizationId) {
        if (enabled) deliver(certificateId, organizationId, Mode.FIRST);
    }

    public DeliveryView resend(UUID certificateId, UUID organizationId) {
        if (!enabled) {
            state(certificateId, organizationId);
            throw new CertificateConflictException("EMAIL_DISABLED", "Email delivery is disabled");
        }
        deliver(certificateId, organizationId, Mode.MANUAL);
        return state(certificateId, organizationId);
    }

    public DeliveryView state(UUID certificateId, UUID organizationId) {
        return transactions.execute(status -> {
            Certificate certificate = certificates.findByIdAndOrganizationId(certificateId, organizationId)
                .orElseThrow(CertificateEmailDeliveryService::notFound);
            EmailDelivery delivery = deliveries.findByCertificateIdAndDeliveryType(certificate.getId(),
                EmailDeliveryType.CERTIFICATE_ISSUED).orElse(null);
            return new DeliveryView(enabled, delivery == null ? null : delivery.getStatus(),
                delivery == null ? 0 : delivery.getAttemptCount(),
                delivery == null ? null : delivery.getSentAt(),
                delivery == null ? null : delivery.getFailureReason(),
                enabled && certificate.getLifecycle() == CertificateLifecycle.ISSUED
                    && certificate.getRevokedAt() == null
                    && delivery != null && delivery.getAttemptCount() < MAX_TOTAL_ATTEMPTS
                    && delivery.getStatus() != EmailDeliveryStatus.PENDING);
        });
    }

    @Scheduled(fixedDelayString = "${app.mail.retry-poll-ms:60000}")
    public void retryDue() {
        if (!enabled) return;
        Instant cutoff = clock.instant().minus(RETRY_DELAY);
        List<UUID> ids = deliveries.findRetryableCertificateIds(
            List.of(EmailDeliveryStatus.FAILED, EmailDeliveryStatus.PENDING),
            MAX_AUTOMATIC_ATTEMPTS, cutoff, PageRequest.of(0, 20)).getContent();
        for (UUID id : ids) {
            try {
                UUID organizationId = transactions.execute(status -> certificates.findById(id)
                    .orElseThrow(CertificateEmailDeliveryService::notFound).getOrganization().getId());
                deliver(id, organizationId, Mode.RETRY);
            } catch (RuntimeException failure) {
                log.warn("Email retry could not complete for certificate {}", id);
            }
        }
    }

    private void deliver(UUID certificateId, UUID organizationId, Mode mode) {
        Prepared prepared = transactions.execute(status -> reserve(certificateId, organizationId, mode));
        if (prepared == null) return;
        try {
            byte[] pdf = artifacts.download(certificateId, organizationId);
            email.send(new CertificateEmailService.IssuanceEmail(prepared.recipientEmail(),
                prepared.recipientName(), prepared.programName(), prepared.organizationName(),
                prepared.publicId(), urls.forCertificate(prepared.publicId()), pdf));
            transactions.executeWithoutResult(status -> deliveries.findById(prepared.deliveryId())
                .orElseThrow().markSent(clock.instant()));
        } catch (RuntimeException failure) {
            // Never persist exception text: SMTP errors may include addresses, server details or credentials.
            transactions.executeWithoutResult(status -> deliveries.findById(prepared.deliveryId())
                .orElseThrow().markFailed("EMAIL_DELIVERY_FAILED"));
            log.warn("Certificate email delivery failed for certificate {} (attempt {})",
                certificateId, prepared.attempt());
        }
    }

    private Prepared reserve(UUID id, UUID organizationId, Mode mode) {
        Certificate certificate = certificates.lockByIdAndOrganizationId(id, organizationId)
            .orElseThrow(CertificateEmailDeliveryService::notFound);
        if (certificate.getLifecycle() != CertificateLifecycle.ISSUED) {
            if (mode == Mode.FIRST) return null;
            throw new CertificateConflictException("CERTIFICATE_NOT_ISSUED", "Certificate has not been issued");
        }
        if (certificate.getRevokedAt() != null) {
            if (mode != Mode.MANUAL) return null;
            throw new CertificateConflictException("CERTIFICATE_REVOKED", "Revoked certificate cannot be emailed");
        }
        if (artifacts.state(id, organizationId).status() != ArtifactState.READY) {
            if (mode == Mode.FIRST || mode == Mode.RETRY) return null;
            throw new CertificateConflictException("ARTIFACT_NOT_READY", "Certificate PDF is not ready");
        }
        EmailDelivery delivery = deliveries.findByCertificateIdAndDeliveryType(id,
            EmailDeliveryType.CERTIFICATE_ISSUED).orElse(null);
        if (mode == Mode.FIRST && delivery != null) return null;
        if (mode == Mode.RETRY && (delivery == null || delivery.getStatus() == EmailDeliveryStatus.SENT
            || delivery.getUpdatedAt().isAfter(clock.instant().minus(RETRY_DELAY)))) return null;
        if (delivery == null) delivery = new EmailDelivery(certificate, EmailDeliveryType.CERTIFICATE_ISSUED,
            certificate.getRecipientEmail());
        if (delivery.getAttemptCount() >= (mode == Mode.MANUAL ? MAX_TOTAL_ATTEMPTS : MAX_AUTOMATIC_ATTEMPTS)) {
            if (mode == Mode.MANUAL) throw new CertificateConflictException("EMAIL_RETRY_LIMIT", "Email resend limit reached");
            return null;
        }
        if (mode == Mode.MANUAL && delivery.getStatus() == EmailDeliveryStatus.PENDING
            && delivery.getAttemptCount() > 0
            && delivery.getUpdatedAt().isAfter(clock.instant().minus(RETRY_DELAY))) {
            throw new CertificateConflictException("EMAIL_IN_PROGRESS", "Email delivery is in progress");
        }
        delivery.beginAttempt(certificate.getRecipientEmail());
        deliveries.saveAndFlush(delivery);
        return new Prepared(delivery.getId(), delivery.getAttemptCount(), certificate.getRecipientEmail(),
            certificate.getRecipientName(), certificate.getProgramName(), certificate.getOrganization().getName(),
            certificate.getCertificateId());
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificate not found");
    }

    private enum Mode { FIRST, RETRY, MANUAL }
    private record Prepared(UUID deliveryId, int attempt, String recipientEmail, String recipientName,
        String programName, String organizationName, String publicId) {}
    public record DeliveryView(boolean enabled, EmailDeliveryStatus status, int attemptCount,
        Instant sentAt, String failureReason, boolean canResend) {}
}
