package com.certchain.certificate;

import com.certchain.auth.AuthenticatedPrincipal;
import com.certchain.certificate.RevocationService.RevokeProgress;
import com.certchain.certificate.dto.RevokeCertificateRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/certificates")
public class RevocationController {
    private final RevocationService service;

    public RevocationController(RevocationService service) { this.service = service; }

    @GetMapping("/{id}/revocation")
    public RevokeProgress progress(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return service.progress(id, principal.organizationId());
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<RevokeProgress> revoke(@PathVariable UUID id,
        @Valid @RequestBody RevokeCertificateRequest request,
        @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return response(service.revoke(id, principal.organizationId(), request.reason()));
    }

    @PostMapping("/{id}/revocation/reconcile")
    public ResponseEntity<RevokeProgress> reconcile(@PathVariable UUID id,
        @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return response(service.reconcile(id, principal.organizationId()));
    }

    private static ResponseEntity<RevokeProgress> response(RevokeProgress progress) {
        return progress.revokedAt() == null && progress.transactionStatus() != null
            && progress.transactionStatus() != com.certchain.transaction.BlockchainTransactionStatus.FAILED
            ? ResponseEntity.accepted().body(progress) : ResponseEntity.ok(progress);
    }
}
