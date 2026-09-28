package com.certchain.certificate;

import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CertificateRepository extends JpaRepository<Certificate, UUID>, JpaSpecificationExecutor<Certificate> {
    Optional<Certificate> findByIdAndOrganizationId(UUID id, UUID organizationId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Certificate c where c.id = :id and c.organization.id = :organizationId")
    Optional<Certificate> lockByIdAndOrganizationId(@Param("id") UUID id,
        @Param("organizationId") UUID organizationId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Certificate c where c.id = :id")
    Optional<Certificate> lockById(@Param("id") UUID id);
    @EntityGraph(attributePaths = "organization")
    Optional<Certificate> findByCertificateId(String certificateId);
    Page<Certificate> findByOrganizationId(UUID organizationId, Pageable pageable);
    @Query("select count(c) from Certificate c where c.organization.id = :organizationId and c.lifecycle = :issued")
    long countIssued(@Param("organizationId") UUID organizationId, @Param("issued") CertificateLifecycle issued);
    @Query("select count(c) from Certificate c where c.organization.id = :organizationId and c.lifecycle = :issued "
        + "and c.revokedAt is null and (c.expiryDate is null or c.expiryDate >= :today)")
    long countValid(@Param("organizationId") UUID organizationId, @Param("issued") CertificateLifecycle issued,
        @Param("today") LocalDate today);
    @Query("select count(c) from Certificate c where c.organization.id = :organizationId and c.lifecycle = :issued "
        + "and c.revokedAt is null and c.expiryDate < :today")
    long countExpired(@Param("organizationId") UUID organizationId, @Param("issued") CertificateLifecycle issued,
        @Param("today") LocalDate today);
    @Query("select count(c) from Certificate c where c.organization.id = :organizationId and c.lifecycle = :issued "
        + "and c.revokedAt is not null")
    long countRevoked(@Param("organizationId") UUID organizationId, @Param("issued") CertificateLifecycle issued);
    @Query("select c from Certificate c where c.organization.id = :organizationId order by c.createdAt desc, c.id desc")
    java.util.List<Certificate> findRecentForOrganization(@Param("organizationId") UUID organizationId, Pageable pageable);

    @Query("""
        select c from Certificate c where c.organization.id = :organizationId
        and (lower(c.recipientName) like lower(concat('%', :query, '%'))
          or lower(c.programName) like lower(concat('%', :query, '%'))
          or lower(c.certificateId) like lower(concat('%', :query, '%')))
        """)
    Page<Certificate> searchByOrganization(@Param("organizationId") UUID organizationId,
        @Param("query") String query, Pageable pageable);
}
