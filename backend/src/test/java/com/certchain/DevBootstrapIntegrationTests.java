package com.certchain;

import com.certchain.auth.DevBootstrap;
import com.certchain.organization.OrganizationRepository;
import com.certchain.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("dev")
@SpringBootTest(properties = {
    "app.auth.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
    "app.bootstrap.enabled=true",
    "app.bootstrap.organization-name=Development Academy",
    "app.bootstrap.organization-email=academy@example.com",
    "app.bootstrap.admin-name=Development Admin",
    "app.bootstrap.admin-email=admin@example.com",
    "app.bootstrap.admin-password=test-password-12345"
})
@Testcontainers(disabledWithoutDocker = true)
class DevBootstrapIntegrationTests {
    @Container @ServiceConnection
    static final PostgreSQLContainer postgres =
        new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));

    @Autowired OrganizationRepository organizations;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired DevBootstrap bootstrap;

    @Test
    void createsOneAdminWithBcryptAndIsIdempotent() {
        var admin = users.findByEmailIgnoreCase("ADMIN@example.com").orElseThrow();
        assertEquals(1, organizations.count());
        assertEquals(1, users.count());
        assertNotEquals("test-password-12345", admin.getPasswordHash());
        assertTrue(passwords.matches("test-password-12345", admin.getPasswordHash()));
        bootstrap.run(null);
        assertEquals(1, organizations.count());
        assertEquals(1, users.count());
    }
}
