package com.certchain.artifact;

import com.certchain.certificate.Certificate;
import com.certchain.certificate.CertificateConflictException;
import com.certchain.certificate.CertificateLifecycle;
import com.certchain.certificate.CertificateRepository;
import com.certchain.certificate.PdfArtifactStatus;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CertificateArtifactService {
    private final CertificateRepository certificates;
    private final ArtifactStorage storage;
    private final CertificatePdfRenderer renderer;

    public CertificateArtifactService(CertificateRepository certificates, ArtifactStorage storage,
                                      CertificatePdfRenderer renderer) {
        this.certificates = certificates;
        this.storage = storage;
        this.renderer = renderer;
    }

    @Transactional
    public ArtifactView ensure(UUID id, UUID organizationId) {
        Certificate certificate = certificates.lockByIdAndOrganizationId(id, organizationId)
            .orElseThrow(CertificateArtifactService::notFound);
        requireIssued(certificate);
        String key = key(certificate.getId());
        try {
            if (certificate.getPdfArtifactStatus() == PdfArtifactStatus.READY
                && certificate.getPdfStorageKey() != null && storage.exists(certificate.getPdfStorageKey())) {
                return new ArtifactView(ArtifactState.READY, null);
            }
            byte[] pdf = renderer.render(certificate);
            storage.write(key, pdf);
        } catch (RuntimeException failure) {
            certificate.markPdfFailed();
            certificates.flush();
            return new ArtifactView(ArtifactState.FAILED, certificate.getPdfArtifactError());
        }
        certificate.markPdfReady(key);
        certificates.flush();
        return new ArtifactView(ArtifactState.READY, null);
    }

    @Transactional(readOnly = true)
    public ArtifactView state(UUID id, UUID organizationId) {
        Certificate certificate = certificates.findByIdAndOrganizationId(id, organizationId)
            .orElseThrow(CertificateArtifactService::notFound);
        if (certificate.getLifecycle() != CertificateLifecycle.ISSUED) {
            return new ArtifactView(ArtifactState.PENDING, null);
        }
        if (certificate.getPdfArtifactStatus() == PdfArtifactStatus.READY
            && certificate.getPdfStorageKey() != null) {
            try {
                if (storage.exists(certificate.getPdfStorageKey())) return new ArtifactView(ArtifactState.READY, null);
            } catch (RuntimeException unavailable) { /* Retry can restore the artifact. */ }
            return new ArtifactView(ArtifactState.FAILED, "Certificate PDF is unavailable; retry is available");
        }
        if (certificate.getPdfArtifactStatus() == PdfArtifactStatus.FAILED) {
            return new ArtifactView(ArtifactState.FAILED, certificate.getPdfArtifactError());
        }
        return new ArtifactView(ArtifactState.PENDING, null);
    }

    @Transactional(readOnly = true)
    public byte[] download(UUID id, UUID organizationId) {
        Certificate certificate = certificates.findByIdAndOrganizationId(id, organizationId)
            .orElseThrow(CertificateArtifactService::notFound);
        requireIssued(certificate);
        if (certificate.getPdfArtifactStatus() != PdfArtifactStatus.READY
            || certificate.getPdfStorageKey() == null) {
            throw new CertificateConflictException("ARTIFACT_NOT_READY", "Certificate PDF is not ready");
        }
        try { return storage.read(certificate.getPdfStorageKey()); }
        catch (RuntimeException unavailable) {
            throw new CertificateConflictException("ARTIFACT_NOT_READY", "Certificate PDF is not ready");
        }
    }

    private static void requireIssued(Certificate certificate) {
        if (certificate.getLifecycle() != CertificateLifecycle.ISSUED) {
            throw new CertificateConflictException("CERTIFICATE_NOT_ISSUED", "Certificate has not been issued");
        }
    }

    private static String key(UUID id) { return "certificates/" + id + "/certificate.pdf"; }
    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificate not found");
    }

    public enum ArtifactState { PENDING, READY, FAILED }
    public record ArtifactView(ArtifactState status, String error) {}
}
