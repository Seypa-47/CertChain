package com.certchain.user;

import com.certchain.common.domain.AuditableEntity;
import com.certchain.organization.Organization;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class AppUser extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @Column(name = "name", nullable = false, length = 200)
    private String name;
    @Column(name = "email", nullable = false, length = 320)
    private String email;
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private UserRole role;
    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    protected AppUser() {}
    public AppUser(Organization organization, String name, String email, String passwordHash, UserRole role) {
        this.organization = java.util.Objects.requireNonNull(organization);
        this.name = java.util.Objects.requireNonNull(name);
        this.email = java.util.Objects.requireNonNull(email);
        this.passwordHash = java.util.Objects.requireNonNull(passwordHash);
        this.role = java.util.Objects.requireNonNull(role);
        this.enabled = true;
    }

    public Organization getOrganization() { return organization; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public UserRole getRole() { return role; }
    public boolean getEnabled() { return enabled; }
    public UUID getId() { return id; }
    public void setName(String value) { this.name = value; }
    public void setEmail(String value) { this.email = value; }
    public void setPasswordHash(String value) { this.passwordHash = value; }
    public void setRole(UserRole value) { this.role = value; }
    public void setEnabled(boolean value) { this.enabled = value; }
}
