package com.certchain.email;

import com.certchain.auth.AuthenticatedPrincipal;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/certificates/{id}/email")
public class CertificateEmailController {
    private final CertificateEmailDeliveryService deliveries;

    public CertificateEmailController(CertificateEmailDeliveryService deliveries) {
        this.deliveries = deliveries;
    }

    @GetMapping
    public CertificateEmailDeliveryService.DeliveryView status(@PathVariable UUID id,
        @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return deliveries.state(id, principal.organizationId());
    }

    @PostMapping("/resend")
    public CertificateEmailDeliveryService.DeliveryView resend(@PathVariable UUID id,
        @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return deliveries.resend(id, principal.organizationId());
    }
}
