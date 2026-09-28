package com.certchain.auth;

import com.certchain.user.UserRole;
import java.util.UUID;

public record AuthenticatedPrincipal(UUID userId, UUID organizationId, UserRole role, String name, String email) {}
