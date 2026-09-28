package com.certchain.organization;

import com.certchain.auth.AuthenticatedPrincipal;
import com.certchain.organization.dto.OrganizationProfile;
import com.certchain.organization.dto.UpdateOrganizationRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organization")
public class OrganizationController {
    private final OrganizationService organizations;

    public OrganizationController(OrganizationService organizations) {
        this.organizations = organizations;
    }

    @GetMapping
    public OrganizationProfile current(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return organizations.current(principal.organizationId());
    }

    @PatchMapping
    public OrganizationProfile update(@Valid @RequestBody UpdateOrganizationRequest request,
        @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return organizations.update(principal.organizationId(), request);
    }
}
