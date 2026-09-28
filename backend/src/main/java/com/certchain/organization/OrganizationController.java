package com.certchain.organization;

import com.certchain.auth.AuthenticatedPrincipal;
import com.certchain.organization.dto.OrganizationSummary;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organization")
public class OrganizationController {
    private final OrganizationRepository organizations;

    public OrganizationController(OrganizationRepository organizations) {
        this.organizations = organizations;
    }

    @GetMapping
    public OrganizationSummary current(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        Organization organization = organizations.findById(principal.organizationId()).orElseThrow();
        return new OrganizationSummary(organization.getId(), organization.getName(), organization.getLogoUrl());
    }
}
