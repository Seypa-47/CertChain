package com.certchain;

import com.certchain.certificate.Certificate;
import com.certchain.certificate.CertificateIdGenerator;
import com.certchain.certificate.CertificateRepository;
import com.certchain.organization.Organization;
import com.certchain.organization.OrganizationRepository;
import com.certchain.transaction.BlockchainTransaction;
import com.certchain.transaction.BlockchainTransactionRepository;
import com.certchain.transaction.BlockchainTransactionType;
import com.certchain.user.AppUser;
import com.certchain.user.AppUserRepository;
import com.certchain.user.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;

/** Explicitly run against an isolated, disposable PostgreSQL database when Docker is unavailable. */
@SpringBootTest(properties = {
    "app.auth.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
    "spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true"
})
@EnabledIfEnvironmentVariable(named = "CERTCHAIN_EXTERNAL_POSTGRES", matches = "true")
class ExternalPostgresIntegrationIT {
    @Autowired JdbcTemplate jdbc;
    @Autowired OrganizationRepository organizations;
    @Autowired AppUserRepository users;
    @Autowired CertificateRepository certificates;
    @Autowired BlockchainTransactionRepository transactions;

    private Organization organization() {
        return organizations.saveAndFlush(new Organization("Integration tenant", UUID.randomUUID() + "@example.test"));
    }

    private Certificate certificate(Organization organization, String id) {
        return certificates.saveAndFlush(new Certificate(id, organization, "Example", "recipient@example.test",
            "Workshop", LocalDate.of(2026, 1, 1)));
    }

    @Test
    void flywayAndHibernateValidatePostgresSchema() {
        assertEquals(1, jdbc.queryForObject(
            "select count(*) from flyway_schema_history where version = '1' and success = true", Integer.class));
        assertNotNull(organization().getId());
    }

    @Test
    void constraintsAndTenantQueriesAreEnforcedByPostgres() {
        Organization first = organization();
        Organization second = organization();
        String email = UUID.randomUUID() + "@example.test";
        users.saveAndFlush(new AppUser(first, "Admin", email, "test-hash", UserRole.ORG_ADMIN));
        assertThrows(DataIntegrityViolationException.class, () -> users.saveAndFlush(
            new AppUser(second, "Duplicate", email.toUpperCase(), "test-hash", UserRole.ORG_ADMIN)));

        String id = new CertificateIdGenerator(jdbc, Clock.systemUTC()).nextId();
        Certificate owned = certificate(first, id);
        assertTrue(certificates.findByIdAndOrganizationId(owned.getId(), first.getId()).isPresent());
        assertTrue(certificates.findByIdAndOrganizationId(owned.getId(), second.getId()).isEmpty());
        assertThrows(DataIntegrityViolationException.class, () -> certificate(second, id));

        Certificate invalid = new Certificate(new CertificateIdGenerator(jdbc, Clock.systemUTC()).nextId(),
            first, "Example", "recipient@example.test", "Workshop", LocalDate.of(2026, 1, 2));
        invalid.setExpiryDate(LocalDate.of(2026, 1, 1));
        assertThrows(DataIntegrityViolationException.class, () -> certificates.saveAndFlush(invalid));

        String hash = "0x" + UUID.randomUUID().toString().replace("-", "").repeat(2);
        BlockchainTransaction tx = new BlockchainTransaction(owned, BlockchainTransactionType.ISSUE,
            "local", 31337, "0x" + "a".repeat(40));
        tx.setTransactionHash(hash);
        transactions.saveAndFlush(tx);
        BlockchainTransaction duplicate = new BlockchainTransaction(owned, BlockchainTransactionType.REVOKE,
            "local", 31337, "0x" + "a".repeat(40));
        duplicate.setTransactionHash(hash);
        assertThrows(DataIntegrityViolationException.class, () -> transactions.saveAndFlush(duplicate));
    }

    @Test
    void globalYearlyAllocationIsUniqueUnderConcurrency() throws Exception {
        CertificateIdGenerator generator = new CertificateIdGenerator(jdbc,
            Clock.fixed(Instant.parse("2097-01-01T00:00:00Z"), ZoneOffset.UTC));
        try (var pool = Executors.newFixedThreadPool(12)) {
            var futures = new java.util.ArrayList<java.util.concurrent.Future<String>>();
            for (int i = 0; i < 60; i++) futures.add(pool.submit(generator::nextId));
            var ids = new HashSet<String>();
            for (var future : futures) ids.add(future.get());
            assertEquals(60, ids.size());
            assertTrue(ids.stream().allMatch(id -> id.matches("CERT-2097-[0-9]{6}")));
        }
    }
}
