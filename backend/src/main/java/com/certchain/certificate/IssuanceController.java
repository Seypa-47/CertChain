package com.certchain.certificate;

import com.certchain.auth.AuthenticatedPrincipal;
import com.certchain.certificate.IssuanceService.IssueProgress;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/certificates")
public class IssuanceController {
    private final IssuanceService service;

    public IssuanceController(IssuanceService service) { this.service = service; }

    @GetMapping("/{id}/issuance")
    public IssueProgress progress(@PathVariable UUID id,
        @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return service.progress(id, principal.organizationId());
    }

    @PostMapping("/{id}/issue")
    public ResponseEntity<IssueProgress> issue(@PathVariable UUID id,
        @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return response(service.issue(id, principal.organizationId()));
    }

    @PostMapping("/{id}/reconcile")
    public ResponseEntity<IssueProgress> reconcile(@PathVariable UUID id,
        @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return response(service.reconcile(id, principal.organizationId()));
    }

    private static ResponseEntity<IssueProgress> response(IssueProgress progress) {
        return progress.lifecycle() == CertificateLifecycle.ISSUING
            ? ResponseEntity.accepted().body(progress) : ResponseEntity.ok(progress);
    }
}
