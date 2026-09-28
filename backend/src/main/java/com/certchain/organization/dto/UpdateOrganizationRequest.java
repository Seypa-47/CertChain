package com.certchain.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateOrganizationRequest(
    @NotBlank @Size(max = 200) String name,
    @NotBlank @Email @Size(max = 320) String email,
    @Pattern(regexp = "^$|^0x[0-9a-fA-F]{40}$", message = "must be a valid Ethereum address")
    String walletAddress
) {}
