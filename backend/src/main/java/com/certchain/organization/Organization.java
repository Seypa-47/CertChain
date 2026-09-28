package com.certchain.organization;

import com.certchain.common.domain.AuditableEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "organization")
public class Organization extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "name", nullable = false, length = 200)
    private String name;
    @Column(name = "email", nullable = false, length = 320)
    private String email;
    @Column(name = "wallet_address", nullable = true, length = 42)
    private String walletAddress;
    @Column(name = "logo_url", nullable = true, length = 2048)
    private String logoUrl;

    protected Organization() {}
    public Organization(String name, String email) {
        this.name = java.util.Objects.requireNonNull(name);
        this.email = java.util.Objects.requireNonNull(email);
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getWalletAddress() { return walletAddress; }
    public String getLogoUrl() { return logoUrl; }
    public UUID getId() { return id; }
    public void setName(String value) { this.name = value; }
    public void setEmail(String value) { this.email = value; }
    public void setWalletAddress(String value) { this.walletAddress = value; }
    public void setLogoUrl(String value) { this.logoUrl = value; }
}
