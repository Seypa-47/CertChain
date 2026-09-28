package com.certchain;

import com.certchain.certificate.*;
import com.certchain.organization.*;
import com.certchain.user.*;
import com.certchain.transaction.*;
import com.certchain.email.*;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true",
    "app.auth.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="})
@Testcontainers(disabledWithoutDocker = true)
class PostgresDomainIntegrationTests {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
        new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));

    @Autowired JdbcTemplate jdbc;
    @Autowired OrganizationRepository organizations;
    @Autowired AppUserRepository users;
    @Autowired CertificateRepository certificates;
    @Autowired BlockchainTransactionRepository transactions;
    @Autowired EmailDeliveryRepository deliveries;
    @Autowired CertificateIdGenerator ids;

    private Organization organization() {
        return organizations.saveAndFlush(new Organization("Tenant " + UUID.randomUUID(), "office@example.com"));
    }

    private Certificate certificate(Organization org, String publicId) {
        return certificates.saveAndFlush(new Certificate(publicId, org, "Recipient", "recipient@example.com",
            "Program", LocalDate.of(2026, 1, 1)));
    }

    @Test
    void migrationAndEntityMappings() {
        assertEquals(4, jdbc.queryForObject("select count(*) from flyway_schema_history where success = true", Integer.class));
        assertEquals(1, jdbc.queryForObject("select count(*) from flyway_schema_history where version = '1' and success = true", Integer.class));
        assertEquals(1, jdbc.queryForObject("select count(*) from flyway_schema_history where version = '4' and success = true", Integer.class));
        Organization org = organization();
        AppUser user = users.saveAndFlush(new AppUser(org, "Admin", UUID.randomUUID() + "@example.com", "hash", UserRole.ORG_ADMIN));
        Certificate cert = certificate(org, ids.nextId());
        BlockchainTransaction tx = transactions.saveAndFlush(new BlockchainTransaction(cert,
            BlockchainTransactionType.ISSUE, "sepolia", 11155111L,
            "0x" + "a".repeat(40)));
        EmailDelivery email = deliveries.saveAndFlush(new EmailDelivery(cert,
            EmailDeliveryType.CERTIFICATE_ISSUED, "recipient@example.com"));
        assertEquals("ORG_ADMIN", jdbc.queryForObject("select role from app_user where id = ?", String.class, user.getId()));
        assertEquals("DRAFT", jdbc.queryForObject("select lifecycle from certificate where id = ?", String.class, cert.getId()));
        assertEquals("CREATED", jdbc.queryForObject("select status from blockchain_transaction where id = ?", String.class, tx.getId()));
        assertEquals("PENDING", jdbc.queryForObject("select status from email_delivery where id = ?", String.class, email.getId()));
    }

    @Test
    void databaseUniquenessAndExpiryChecks() {
        Organization org = organization();
        Organization other = organization();
        String email = UUID.randomUUID() + "@example.com";
        users.saveAndFlush(new AppUser(org, "Admin", email, "hash", UserRole.ORG_ADMIN));
        assertTrue(users.findByEmailIgnoreCase(email.toUpperCase()).isPresent());
        assertThrows(DataIntegrityViolationException.class, () ->
            users.saveAndFlush(new AppUser(other, "Duplicate", email.toUpperCase(), "hash", UserRole.ORG_ADMIN)));
        String id = ids.nextId();
        certificate(org, id);
        assertThrows(DataIntegrityViolationException.class, () -> certificate(other, id));
        Certificate bad = new Certificate(ids.nextId(), org, "Bad", "bad@example.com", "Course",
            LocalDate.of(2026, 2, 1));
        bad.setExpiryDate(LocalDate.of(2026, 1, 1));
        assertThrows(DataIntegrityViolationException.class, () -> certificates.saveAndFlush(bad));
    }

    @Test
    void tenantScopedQueries() {
        Organization first = organization();
        Organization second = organization();
        Certificate cert = certificate(first, ids.nextId());
        Certificate other = certificate(second, ids.nextId());
        assertTrue(certificates.findByIdAndOrganizationId(cert.getId(), first.getId()).isPresent());
        assertTrue(certificates.findByIdAndOrganizationId(cert.getId(), second.getId()).isEmpty());
        assertTrue(certificates.findByIdAndOrganizationId(other.getId(), first.getId()).isEmpty());
        assertEquals(1, certificates.findByOrganizationId(second.getId(), Pageable.unpaged()).getTotalElements());
        assertEquals(1, certificates.searchByOrganization(first.getId(), "recipient",
            Pageable.unpaged()).getTotalElements());
        assertEquals(0, certificates.searchByOrganization(second.getId(), cert.getCertificateId(),
            Pageable.unpaged()).getTotalElements());
    }

    @Test
    void emailDeliveryHasOneJournalPerCertificateAndType() {
        Certificate cert = certificate(organization(), ids.nextId());
        deliveries.saveAndFlush(new EmailDelivery(cert, EmailDeliveryType.CERTIFICATE_ISSUED,
            "recipient@example.com"));
        assertThrows(DataIntegrityViolationException.class, () -> deliveries.saveAndFlush(
            new EmailDelivery(cert, EmailDeliveryType.CERTIFICATE_ISSUED, "recipient@example.com")));
    }

    @Test
    void allocationFormatYearsAndConcurrency() throws Exception {
        CertificateIdGenerator fixed = new CertificateIdGenerator(jdbc,
            Clock.fixed(Instant.parse("2077-06-01T00:00:00Z"), ZoneOffset.UTC));
        assertEquals("CERT-2077-000001", fixed.nextId());
        assertEquals("CERT-2077-000002", fixed.nextId());
        CertificateIdGenerator nextYear = new CertificateIdGenerator(jdbc,
            Clock.fixed(Instant.parse("2078-01-01T00:00:00Z"), ZoneOffset.UTC));
        assertEquals("CERT-2078-000001", nextYear.nextId());
        try (var pool = Executors.newFixedThreadPool(12)) {
            var futures = new java.util.ArrayList<java.util.concurrent.Future<String>>();
            for (int i = 0; i < 60; i++) futures.add(pool.submit(fixed::nextId));
            var values = new HashSet<String>();
            for (var future : futures) values.add(future.get());
            assertEquals(60, values.size());
            assertTrue(values.stream().allMatch(v -> v.matches("CERT-2077-[0-9]{6}")));
        }
    }

    @Test
    void lastCertificateNumberIsIssuedOnce() {
        int year = 2080;
        jdbc.update("insert into certificate_number_sequence (sequence_year, next_value, version) values (?, 999999, 0)", year);
        CertificateIdGenerator fixed = new CertificateIdGenerator(jdbc,
            Clock.fixed(Instant.parse("2080-06-01T00:00:00Z"), ZoneOffset.UTC));
        assertEquals("CERT-2080-999999", fixed.nextId());
        assertThrows(IllegalStateException.class, fixed::nextId);
        assertEquals(1000000L, jdbc.queryForObject(
            "select next_value from certificate_number_sequence where sequence_year = ?", Long.class, year));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
            "update certificate_number_sequence set next_value = 1000001 where sequence_year = ?", year));
    }

    @Test
    void optimisticLockingAndTransactionHashUniqueness() {
        Organization org = organization();
        Certificate cert = certificate(org, ids.nextId());
        int changed = jdbc.update("update certificate set version = version + 1 where id = ?", cert.getId());
        assertEquals(1, changed);
        cert.setDescription("stale change");
        assertThrows(ObjectOptimisticLockingFailureException.class,
            () -> certificates.saveAndFlush(cert));
        BlockchainTransaction first = new BlockchainTransaction(cert, BlockchainTransactionType.ISSUE,
            "sepolia", 11155111L, "0x" + "b".repeat(40));
        first.setTransactionHash("0x" + "c".repeat(64));
        transactions.saveAndFlush(first);
        Certificate other = certificate(organization(), ids.nextId());
        BlockchainTransaction second = new BlockchainTransaction(other, BlockchainTransactionType.ISSUE,
            "sepolia", 11155111L, "0x" + "b".repeat(40));
        second.setTransactionHash(first.getTransactionHash());
        assertThrows(DataIntegrityViolationException.class, () -> transactions.saveAndFlush(second));
    }
}
