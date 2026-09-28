package com.certchain.email;

import java.util.UUID;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailDeliveryRepository extends JpaRepository<EmailDelivery, UUID> {
    Page<EmailDelivery> findByStatusOrderByCreatedAtAsc(EmailDeliveryStatus status, Pageable pageable);
    Optional<EmailDelivery> findByCertificateIdAndDeliveryType(UUID certificateId, EmailDeliveryType deliveryType);
    @Query("select e.certificate.id from EmailDelivery e where e.attemptCount < :limit and e.updatedAt < :before "
        + "and e.status in :statuses and e.certificate.revokedAt is null order by e.updatedAt asc")
    Page<UUID> findRetryableCertificateIds(@Param("statuses") java.util.Collection<EmailDeliveryStatus> statuses,
        @Param("limit") int limit, @Param("before") Instant before, Pageable pageable);
}
