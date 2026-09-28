package com.certchain.certificate;

import com.certchain.auth.AuthenticatedPrincipal;
import com.certchain.certificate.dto.CertificatePage;
import com.certchain.certificate.dto.CertificateResponse;
import com.certchain.certificate.dto.CreateCertificateRequest;
import com.certchain.certificate.dto.UpdateCertificateRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/certificates")
public class CertificateDraftController {
    private final CertificateDraftService drafts;

    public CertificateDraftController(CertificateDraftService drafts) { this.drafts = drafts; }

    @PostMapping
    public ResponseEntity<CertificateResponse> create(@Valid @RequestBody CreateCertificateRequest request,
        @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        CertificateResponse result = drafts.create(principal.organizationId(), request);
        return ResponseEntity.created(URI.create("/api/certificates/" + result.id())).body(result);
    }

    @GetMapping
    public CertificatePage list(@AuthenticationPrincipal AuthenticatedPrincipal principal,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "") String query,
        @RequestParam(required = false) CertificateLifecycle lifecycle,
        @RequestParam(defaultValue = "createdAt") String sort,
        @RequestParam(defaultValue = "desc") String direction) {
        return drafts.list(principal.organizationId(), page, size, query, lifecycle, sort, direction);
    }

    @PatchMapping("/{id}")
    public CertificateResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateCertificateRequest request,
        @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return drafts.update(principal.organizationId(), id, request);
    }
}
