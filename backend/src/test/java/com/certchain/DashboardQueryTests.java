package com.certchain;

import static org.junit.jupiter.api.Assertions.*;

import com.certchain.certificate.Certificate;
import com.certchain.certificate.CertificateRepository;
import com.certchain.certificate.CertificateStatus;
import com.certchain.dashboard.DashboardService;
import com.certchain.organization.Organization;
import com.certchain.organization.OrganizationRepository;
import com.certchain.organization.OrganizationService;
import com.certchain.organization.dto.UpdateOrganizationRequest;
import com.certchain.transaction.BlockchainTransaction;
import com.certchain.transaction.BlockchainTransactionRepository;
import com.certchain.transaction.BlockchainTransactionType;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:dashboard;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class DashboardQueryTests {
    private static final AtomicInteger ids = new AtomicInteger(100);
    @Autowired OrganizationRepository organizations;
    @Autowired CertificateRepository certificates;
    @Autowired BlockchainTransactionRepository transactions;
    @Autowired DashboardService dashboard;
    @Autowired Clock clock;
    @Autowired OrganizationService profiles;
    @Autowired Validator validator;

    private Certificate certificate(Organization organization, LocalDate expiry, boolean revoked) {
        Certificate certificate = new Certificate("CERT-2026-" + String.format("%06d", ids.incrementAndGet()),
            organization, "Recipient", "recipient@example.test", "Program", LocalDate.now(clock).minusDays(30));
        certificate.setExpiryDate(expiry);
        certificate.beginIssuance("a".repeat(64), "v1");
        certificate.markIssued(Instant.now(clock));
        if (revoked) {
            certificate.setRevocationReason("Private correction");
            certificate.markRevoked(Instant.now(clock));
        }
        return certificates.saveAndFlush(certificate);
    }

    @Test
    void countsOnlyTenantIssuedRecordsWithRevocationPriorityAndExpiryBoundary() {
        Organization first = organizations.saveAndFlush(new Organization("First", "first@example.test"));
        Organization second = organizations.saveAndFlush(new Organization("Second", "second@example.test"));
        LocalDate today = LocalDate.now(clock);
        certificate(first, today, false);
        certificate(first, today.minusDays(1), false);
        certificate(first, today.minusDays(1), true);
        Certificate foreign = certificate(second, null, false);
        certificates.saveAndFlush(new Certificate("CERT-2026-999998", first, "Draft", "draft@example.test",
            "Program", today));

        var result = dashboard.get(first.getId());
        assertEquals(3, result.totalIssued());
        assertEquals(1, result.valid());
        assertEquals(1, result.expired());
        assertEquals(1, result.revoked());
        assertEquals(4, result.recentCertificates().size());
        assertTrue(result.recentCertificates().stream().anyMatch(item -> item.publicStatus() == CertificateStatus.REVOKED));
        assertTrue(result.recentCertificates().stream().noneMatch(item -> item.id().equals(foreign.getId())));
    }

    @Test
    void recentTransactionsStayWithinTenant() {
        Organization first = organizations.saveAndFlush(new Organization("First", "first@example.test"));
        Organization second = organizations.saveAndFlush(new Organization("Second", "second@example.test"));
        Certificate owned = certificate(first, null, false);
        Certificate foreign = certificate(second, null, false);
        transactions.saveAndFlush(new BlockchainTransaction(owned, BlockchainTransactionType.ISSUE,
            "local", 31337, "0x" + "a".repeat(40)));
        transactions.saveAndFlush(new BlockchainTransaction(foreign, BlockchainTransactionType.ISSUE,
            "local", 31337, "0x" + "a".repeat(40)));
        var result = dashboard.get(first.getId());
        assertEquals(1, result.recentTransactions().size());
        assertEquals(owned.getId(), result.recentTransactions().getFirst().certificateId());
    }

    @Test
    void profileUpdateNormalizesAllowedFieldsAndKeepsOtherTenantUntouched() {
        Organization first = organizations.saveAndFlush(new Organization("First", "first@example.test"));
        Organization second = organizations.saveAndFlush(new Organization("Second", "second@example.test"));
        assertFalse(validator.validate(new UpdateOrganizationRequest("School", "school@example.test",
            "invalid-wallet")).isEmpty());
        var updated = profiles.update(first.getId(), new UpdateOrganizationRequest("  New   School  ",
            "  OFFICE@EXAMPLE.TEST  ", ""));
        assertEquals("New School", updated.name());
        assertEquals("office@example.test", updated.email());
        assertNull(updated.walletAddress());
        assertEquals("Second", profiles.current(second.getId()).name());
    }
}
