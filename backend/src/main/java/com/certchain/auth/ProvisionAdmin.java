package com.certchain.auth;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** One-time operator command. It is never registered as an application startup bean. */
public final class ProvisionAdmin {
    private ProvisionAdmin() { }

    public static void main(String[] args) throws SQLException {
        provision(System.getenv());
    }

    static void provision(Map<String, String> environment) throws SQLException {
        if (!"true".equals(environment.get("PROVISION_ADMIN_ENABLED")))
            throw new IllegalStateException("Set PROVISION_ADMIN_ENABLED=true for the one-time command");
        String url = required(environment, "DATABASE_URL");
        String username = required(environment, "DATABASE_USERNAME");
        String password = required(environment, "DATABASE_PASSWORD");
        String orgName = required(environment, "PROVISION_ORGANIZATION_NAME").trim();
        String orgEmail = email(environment, "PROVISION_ORGANIZATION_EMAIL");
        String adminName = required(environment, "PROVISION_ADMIN_NAME").trim();
        String adminEmail = email(environment, "PROVISION_ADMIN_EMAIL");
        String adminPassword = required(environment, "PROVISION_ADMIN_PASSWORD");
        if (!url.startsWith("jdbc:postgresql://") || !url.matches(".*[?&]sslmode=(require|verify-ca|verify-full)(?:&.*)?$")
            || adminPassword.length() < 12
            || orgName.length() > 200 || adminName.length() > 200)
            throw new IllegalArgumentException("Provisioning requires PostgreSQL TLS and valid account values");

        Flyway.configure().dataSource(url, username, password).load().migrate();
        try (var connection = DriverManager.getConnection(url, username, password)) {
            connection.setAutoCommit(false);
            try {
                try (var check = connection.prepareStatement("SELECT 1 FROM app_user WHERE lower(email) = ?")) {
                    check.setString(1, adminEmail);
                    try (var found = check.executeQuery()) {
                        if (found.next()) throw new IllegalStateException("Admin email already exists");
                    }
                }
                UUID organizationId;
                try (var find = connection.prepareStatement("SELECT id FROM organization WHERE lower(email) = ?")) {
                    find.setString(1, orgEmail);
                    try (var found = find.executeQuery()) {
                        organizationId = found.next() ? found.getObject(1, UUID.class) : null;
                        if (organizationId != null && found.next())
                            throw new IllegalStateException("Organization email is ambiguous");
                    }
                }
                OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
                if (organizationId == null) {
                    organizationId = UUID.randomUUID();
                    try (var create = connection.prepareStatement(
                        "INSERT INTO organization (id, name, email, created_at, updated_at) VALUES (?, ?, ?, ?, ?)")) {
                        create.setObject(1, organizationId);
                        create.setString(2, orgName);
                        create.setString(3, orgEmail);
                        create.setObject(4, now);
                        create.setObject(5, now);
                        create.executeUpdate();
                    }
                }
                try (var create = connection.prepareStatement(
                    "INSERT INTO app_user (id, organization_id, name, email, password_hash, role, enabled, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, 'ORG_ADMIN', true, ?, ?)")) {
                    create.setObject(1, UUID.randomUUID());
                    create.setObject(2, organizationId);
                    create.setString(3, adminName);
                    create.setString(4, adminEmail);
                    create.setString(5, new BCryptPasswordEncoder().encode(adminPassword));
                    create.setObject(6, now);
                    create.setObject(7, now);
                    create.executeUpdate();
                }
                connection.commit();
                System.out.println("One organization admin provisioned. Disable and remove the one-time provisioning variables.");
            } catch (Exception failure) {
                connection.rollback();
                throw failure;
            }
        }
    }

    private static String required(Map<String, String> environment, String key) {
        String value = environment.get(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(key + " is required");
        return value;
    }

    private static String email(Map<String, String> environment, String key) {
        String value = required(environment, key).trim().toLowerCase(Locale.ROOT);
        if (value.length() > 320 || !value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw new IllegalArgumentException(key + " must be an email address");
        return value;
    }
}
