package com.certchain.certificate;

import com.certchain.auth.AuthenticatedPrincipal;
import com.certchain.certificate.dto.CertificateResponse;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/certificates")
public class CertificateReadController {
    private final CertificateDraftService drafts;

    public CertificateReadController(CertificateDraftService drafts) { this.drafts = drafts; }

    @GetMapping("/{id}")
    public CertificateResponse get(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return drafts.get(principal.organizationId(), id);
    }
}
