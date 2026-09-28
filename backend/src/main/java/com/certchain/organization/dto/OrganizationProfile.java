package com.certchain.organization.dto;

import com.certchain.organization.Organization;
import java.util.UUID;

public record OrganizationProfile(UUID id, String name, String email, String walletAddress, String logoUrl) {
    public static OrganizationProfile from(Organization organization) {
        return new OrganizationProfile(organization.getId(), organization.getName(), organization.getEmail(),
            organization.getWalletAddress(), organization.getLogoUrl());
    }
}
