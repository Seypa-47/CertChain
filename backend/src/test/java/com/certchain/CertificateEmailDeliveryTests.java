package com.certchain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.certchain.artifact.CertificateArtifactService;
import com.certchain.artifact.VerificationUrlFactory;
import com.certchain.certificate.Certificate;
import com.certchain.certificate.CertificateLifecycle;
import com.certchain.certificate.CertificateRepository;
import com.certchain.email.CertificateEmailDeliveryService;
import com.certchain.email.CertificateEmailService;
import com.certchain.email.EmailDelivery;
import com.certchain.email.EmailDeliveryRepository;
import com.certchain.email.EmailDeliveryStatus;
import com.certchain.email.EmailDeliveryType;
import com.certchain.organization.Organization;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class CertificateEmailDeliveryTests {
    private final UUID certificateId = UUID.randomUUID();
    private final UUID organizationId = UUID.randomUUID();
    private final CertificateRepository certificates = mock(CertificateRepository.class);
    private final EmailDeliveryRepository deliveries = mock(EmailDeliveryRepository.class);
    private final CertificateArtifactService artifacts = mock(CertificateArtifactService.class);
    private final CertificateEmailService sender = mock(CertificateEmailService.class);
    private final TransactionTemplate transactions = mock(TransactionTemplate.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-28T00:00:00Z"), ZoneOffset.UTC);
    private final AtomicReference<EmailDelivery> journal = new AtomicReference<>();
    private Certificate certificate;
    private CertificateEmailDeliveryService service;

    @BeforeEach
    void setup() {
        Organization organization = new Organization("Example University", "org@example.test");
        ReflectionTestUtils.setField(organization, "id", organizationId);
        certificate = new Certificate("CERT-2026-000001", organization, "Alice", "alice@example.test",
            "Security", LocalDate.of(2026, 9, 1));
        ReflectionTestUtils.setField(certificate, "id", certificateId);
        ReflectionTestUtils.setField(certificate, "lifecycle", CertificateLifecycle.ISSUED);
        when(certificates.lockByIdAndOrganizationId(certificateId, organizationId))
            .thenReturn(Optional.of(certificate));
        when(certificates.findByIdAndOrganizationId(certificateId, organizationId))
            .thenReturn(Optional.of(certificate));
        when(artifacts.state(certificateId, organizationId)).thenReturn(
            new CertificateArtifactService.ArtifactView(CertificateArtifactService.ArtifactState.READY, null));
        when(artifacts.download(certificateId, organizationId)).thenReturn("%PDF".getBytes());
        when(deliveries.findByCertificateIdAndDeliveryType(certificateId, EmailDeliveryType.CERTIFICATE_ISSUED))
            .thenAnswer(invocation -> Optional.ofNullable(journal.get()));
        when(deliveries.saveAndFlush(any())).thenAnswer(invocation -> {
            EmailDelivery entry = invocation.getArgument(0);
            ReflectionTestUtils.setField(entry, "id", UUID.randomUUID());
            journal.set(entry);
            return entry;
        });
        when(deliveries.findById(any())).thenAnswer(invocation -> Optional.ofNullable(journal.get()));
        when(transactions.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
        doAnswer(invocation -> {
            java.util.function.Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactions).executeWithoutResult(any());
        service = new CertificateEmailDeliveryService(certificates, deliveries, artifacts,
            new VerificationUrlFactory("https://certchain.example"), sender, transactions, clock, true);
    }

    @Test
    void sendsOnceToStoredRecipientWithBackendUrlAndPdf() {
        service.deliverAfterArtifact(certificateId, organizationId);
        service.deliverAfterArtifact(certificateId, organizationId);
        ArgumentCaptor<CertificateEmailService.IssuanceEmail> sent =
            ArgumentCaptor.forClass(CertificateEmailService.IssuanceEmail.class);
        verify(sender, times(1)).send(sent.capture());
        assertEquals("alice@example.test", sent.getValue().recipientEmail());
        assertEquals("https://certchain.example/verify/CERT-2026-000001", sent.getValue().verificationUrl());
        assertArrayEquals("%PDF".getBytes(), sent.getValue().pdf());
        assertEquals(EmailDeliveryStatus.SENT, journal.get().getStatus());
        assertEquals(1, journal.get().getAttemptCount());
    }

    @Test
    void smtpFailureIsSanitizedAndDoesNotChangeIssuance() {
        doThrow(new IllegalStateException("secret smtp password and recipient"))
            .when(sender).send(any());
        service.deliverAfterArtifact(certificateId, organizationId);
        assertEquals(EmailDeliveryStatus.FAILED, journal.get().getStatus());
        assertEquals("EMAIL_DELIVERY_FAILED", journal.get().getFailureReason());
        assertEquals(CertificateLifecycle.ISSUED, certificate.getLifecycle());
        assertEquals(1, journal.get().getAttemptCount());
    }

    @Test
    void manualResendUsesStoredRecipientAndIsBounded() {
        service.deliverAfterArtifact(certificateId, organizationId);
        for (int attempt = 2; attempt <= 5; attempt++) {
            service.resend(certificateId, organizationId);
            assertEquals(attempt, journal.get().getAttemptCount());
        }
        assertEquals(EmailDeliveryStatus.SENT, journal.get().getStatus());
        verify(sender, times(5)).send(any());
        var limit = assertThrows(com.certchain.certificate.CertificateConflictException.class,
            () -> service.resend(certificateId, organizationId));
        assertEquals("EMAIL_RETRY_LIMIT", limit.code());
    }

    @Test
    void missingArtifactPreventsEmailAndJournalCreation() {
        when(artifacts.state(certificateId, organizationId)).thenReturn(
            new CertificateArtifactService.ArtifactView(CertificateArtifactService.ArtifactState.FAILED, "failed"));
        service.deliverAfterArtifact(certificateId, organizationId);
        verifyNoInteractions(sender);
        assertNull(journal.get());
    }

    @Test
    void draftCannotTriggerRecipientEmail() {
        ReflectionTestUtils.setField(certificate, "lifecycle", CertificateLifecycle.DRAFT);
        service.deliverAfterArtifact(certificateId, organizationId);
        verifyNoInteractions(sender);
        assertNull(journal.get());
    }

    @Test
    void retriesFailedDeliveryAfterDelay() {
        doThrow(new IllegalStateException("SMTP unavailable")).doNothing().when(sender).send(any());
        service.deliverAfterArtifact(certificateId, organizationId);
        assertEquals(EmailDeliveryStatus.FAILED, journal.get().getStatus());
        ReflectionTestUtils.setField(journal.get(), "updatedAt", clock.instant().minusSeconds(180));
        when(deliveries.findRetryableCertificateIds(any(), eq(3), any(), any()))
            .thenReturn(new PageImpl<>(java.util.List.of(certificateId)));
        when(certificates.findById(certificateId)).thenReturn(Optional.of(certificate));
        service.retryDue();
        assertEquals(EmailDeliveryStatus.SENT, journal.get().getStatus());
        assertEquals(2, journal.get().getAttemptCount());
        verify(sender, times(2)).send(any());
    }

    @Test
    void crossTenantResendCannotReadOrSend() {
        UUID differentTenant = UUID.randomUUID();
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
            () -> service.resend(certificateId, differentTenant));
        verifyNoInteractions(sender);
    }
}
