package com.certchain.auth.dto;

import com.certchain.auth.AuthenticatedPrincipal;
import com.certchain.user.UserRole;
import java.util.UUID;

public record UserResponse(UUID id, UUID organizationId, String name, String email, UserRole role) {
    public static UserResponse from(AuthenticatedPrincipal principal) {
        return new UserResponse(principal.userId(), principal.organizationId(),
            principal.name(), principal.email(), principal.role());
    }
}
