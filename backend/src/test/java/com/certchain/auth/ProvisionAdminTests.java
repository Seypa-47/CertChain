package com.certchain.auth;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProvisionAdminTests {
    @Test
    void requiresExplicitFlagBeforeAnyDatabaseAccess() {
        assertThrows(IllegalStateException.class, () -> ProvisionAdmin.provision(Map.of()));
    }

    @Test
    void rejectsInsecureDatabaseAndWeakAccountValuesBeforeConnecting() {
        var values = new HashMap<String, String>();
        values.put("PROVISION_ADMIN_ENABLED", "true");
        values.put("DATABASE_URL", "jdbc:postgresql://example.test/certchain");
        values.put("DATABASE_USERNAME", "operator");
        values.put("DATABASE_PASSWORD", "database-password");
        values.put("PROVISION_ORGANIZATION_NAME", "Example Academy");
        values.put("PROVISION_ORGANIZATION_EMAIL", "academy@example.test");
        values.put("PROVISION_ADMIN_NAME", "Administrator");
        values.put("PROVISION_ADMIN_EMAIL", "admin@example.test");
        values.put("PROVISION_ADMIN_PASSWORD", "strong-admin-passphrase");
        assertThrows(IllegalArgumentException.class, () -> ProvisionAdmin.provision(values));
        values.put("DATABASE_URL", "jdbc:postgresql://example.test/certchain?sslmode=require");
        values.put("PROVISION_ADMIN_PASSWORD", "short");
        assertThrows(IllegalArgumentException.class, () -> ProvisionAdmin.provision(values));
        values.put("PROVISION_ADMIN_PASSWORD", "strong-admin-passphrase");
        values.put("PROVISION_ADMIN_EMAIL", "invalid-email");
        assertThrows(IllegalArgumentException.class, () -> ProvisionAdmin.provision(values));
    }
}
