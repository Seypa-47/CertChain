package com.certchain.organization;

import com.certchain.organization.dto.OrganizationProfile;
import com.certchain.organization.dto.UpdateOrganizationRequest;
import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrganizationService {
    private final OrganizationRepository organizations;
    public OrganizationService(OrganizationRepository organizations) { this.organizations = organizations; }

    @Transactional(readOnly = true)
    public OrganizationProfile current(UUID organizationId) {
        return OrganizationProfile.from(organizations.findById(organizationId).orElseThrow(OrganizationService::notFound));
    }

    @Transactional
    public OrganizationProfile update(UUID organizationId, UpdateOrganizationRequest request) {
        Organization organization = organizations.findById(organizationId).orElseThrow(OrganizationService::notFound);
        String name = Normalizer.normalize(request.name().strip(), Normalizer.Form.NFC).replaceAll("\\s+", " ");
        String email = Normalizer.normalize(request.email().strip(), Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
        if (name.isBlank() || name.length() > 200 || email.length() > 320) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid organization profile");
        }
        organization.setName(name);
        organization.setEmail(email);
        organization.setWalletAddress(request.walletAddress() == null || request.walletAddress().isBlank()
            ? null : request.walletAddress());
        organizations.flush();
        return OrganizationProfile.from(organization);
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found");
    }
}
