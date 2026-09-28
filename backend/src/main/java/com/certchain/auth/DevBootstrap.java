package com.certchain.auth;

import com.certchain.organization.Organization;
import com.certchain.organization.OrganizationRepository;
import com.certchain.user.AppUser;
import com.certchain.user.AppUserRepository;
import com.certchain.user.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "app.bootstrap", name = "enabled", havingValue = "true")
public class DevBootstrap implements ApplicationRunner {
    private final OrganizationRepository organizations;
    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    @Value("${app.bootstrap.organization-name}") private String organizationName;
    @Value("${app.bootstrap.organization-email}") private String organizationEmail;
    @Value("${app.bootstrap.admin-name}") private String adminName;
    @Value("${app.bootstrap.admin-email}") private String adminEmail;
    @Value("${app.bootstrap.admin-password}") private String adminPassword;

    public DevBootstrap(OrganizationRepository organizations, AppUserRepository users, PasswordEncoder encoder) {
        this.organizations = organizations;
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (organizationName.isBlank() || organizationEmail.isBlank() || adminName.isBlank()
            || adminEmail.isBlank() || adminPassword.length() < 12) {
            throw new IllegalStateException("Development bootstrap requires organization/admin values and a password of at least 12 characters");
        }
        if (users.findByEmailIgnoreCase(adminEmail).isPresent()) return;
        Organization organization = organizations.findFirstByEmailIgnoreCase(organizationEmail)
            .orElseGet(() -> organizations.save(new Organization(organizationName, organizationEmail)));
        users.save(new AppUser(organization, adminName, adminEmail, encoder.encode(adminPassword), UserRole.ORG_ADMIN));
    }
}
