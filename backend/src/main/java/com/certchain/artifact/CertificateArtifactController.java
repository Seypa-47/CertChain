package com.certchain.artifact;

import com.certchain.auth.AuthenticatedPrincipal;
import com.certchain.certificate.CertificateRepository;
import com.certchain.email.CertificateEmailDeliveryService;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/certificates")
public class CertificateArtifactController {
    private final CertificateArtifactService artifacts;
    private final CertificateRepository certificates;
    private final CertificateEmailDeliveryService emails;

    public CertificateArtifactController(CertificateArtifactService artifacts,
                                         CertificateRepository certificates, CertificateEmailDeliveryService emails) {
        this.artifacts = artifacts;
        this.certificates = certificates;
        this.emails = emails;
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> download(@PathVariable UUID id,
        @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        byte[] content = artifacts.download(id, principal.organizationId());
        String publicId = certificates.findByIdAndOrganizationId(id, principal.organizationId())
            .orElseThrow().getCertificateId();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
            .cacheControl(CacheControl.noStore())
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + publicId + ".pdf\"")
            .body(content);
    }

    @GetMapping("/{id}/pdf/status")
    public CertificateArtifactService.ArtifactView status(@PathVariable UUID id,
        @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return artifacts.state(id, principal.organizationId());
    }

    @PostMapping("/{id}/pdf/retry")
    public CertificateArtifactService.ArtifactView retry(@PathVariable UUID id,
        @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        var result = artifacts.ensure(id, principal.organizationId());
        if (result.status() == CertificateArtifactService.ArtifactState.READY) {
            emails.deliverAfterArtifact(id, principal.organizationId());
        }
        return result;
    }
}
