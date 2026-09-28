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
    List<BlockchainTransaction> findByCertificateAndTransactionTypeOrderByCreatedAtDesc(
        Certificate certificate, BlockchainTransactionType transactionType);
    List<BlockchainTransaction> findByCertificateIdAndTransactionTypeOrderByCreatedAtDesc(
        UUID certificateId, BlockchainTransactionType transactionType);
}
