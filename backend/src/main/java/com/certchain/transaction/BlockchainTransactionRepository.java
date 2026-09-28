package com.certchain.transaction;

import com.certchain.certificate.Certificate;
import com.certchain.certificate.CertificateLifecycle;
import java.util.List;
import java.util.UUID;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

public interface BlockchainTransactionRepository extends JpaRepository<BlockchainTransaction, UUID> {
    java.util.Optional<BlockchainTransaction> findFirstByCertificateIdAndTransactionTypeOrderByCreatedAtDesc(
        UUID certificateId, BlockchainTransactionType transactionType);
    List<BlockchainTransaction> findByTransactionTypeAndStatusIn(BlockchainTransactionType type,
        Set<BlockchainTransactionStatus> statuses);
    @Query("select t from BlockchainTransaction t where t.transactionType = :type "
        + "and t.certificate.lifecycle <> :issued and t.status in :statuses order by t.createdAt asc")
    List<BlockchainTransaction> findUnresolvedIssues(
        @Param("type") BlockchainTransactionType type,
        @Param("issued") CertificateLifecycle issued,
        @Param("statuses") Set<BlockchainTransactionStatus> statuses, Pageable pageable);
    @Query("select t from BlockchainTransaction t where t.transactionType = :type "
        + "and t.certificate.revokedAt is null and t.status in :statuses order by t.createdAt asc")
    List<BlockchainTransaction> findUnresolvedRevocations(@Param("type") BlockchainTransactionType type,
        @Param("statuses") Set<BlockchainTransactionStatus> statuses, Pageable pageable);
    List<BlockchainTransaction> findByCertificateAndTransactionTypeOrderByCreatedAtDesc(
        Certificate certificate, BlockchainTransactionType transactionType);
    List<BlockchainTransaction> findByCertificateIdAndTransactionTypeOrderByCreatedAtDesc(
        UUID certificateId, BlockchainTransactionType transactionType);
    @Query("select t from BlockchainTransaction t join fetch t.certificate c "
        + "where c.organization.id = :organizationId order by t.createdAt desc, t.id desc")
    List<BlockchainTransaction> findRecentForOrganization(@Param("organizationId") UUID organizationId, Pageable pageable);
}
