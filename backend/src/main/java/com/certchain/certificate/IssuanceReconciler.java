package com.certchain.certificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "app.blockchain", name = "enabled", havingValue = "true")
public class IssuanceReconciler {
    private static final Logger log = LoggerFactory.getLogger(IssuanceReconciler.class);
    private final IssuanceService service;
    private final IssuancePersistence journal;
    private final RevocationService revocations;
    private final RevocationPersistence revocationJournal;

    public IssuanceReconciler(IssuanceService service, IssuancePersistence journal,
                              RevocationService revocations, RevocationPersistence revocationJournal) {
        this.service = service;
        this.journal = journal;
        this.revocations = revocations;
        this.revocationJournal = revocationJournal;
    }

    @Scheduled(fixedDelayString = "${app.blockchain.reconcile-interval:PT1M}")
    public void reconcile() {
        for (var certificateId : journal.unresolvedCertificateIds()) {
            try { service.reconcileBackground(certificateId); }
            catch (RuntimeException unavailable) {
                log.warn("Reconciliation deferred for certificate {}", certificateId);
            }
        }
        for (var certificateId : revocationJournal.unresolvedIds()) {
            try { revocations.reconcileBackground(certificateId); }
            catch (RuntimeException unavailable) {
                log.warn("Revocation reconciliation deferred for certificate {}", certificateId);
            }
        }
    }
}
