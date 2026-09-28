package com.certchain.certificate;

import com.certchain.auth.AuthenticatedPrincipal;
import com.certchain.certificate.dto.CertificateMapper;
import com.certchain.certificate.dto.CertificateResponse;
import com.certchain.transaction.BlockchainTransaction;
import com.certchain.transaction.BlockchainTransactionRepository;
import com.certchain.transaction.BlockchainTransactionType;
import java.util.ArrayList;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/certificates")
public class CertificateReadController {
    private final CertificateRepository certificates;
    private final BlockchainTransactionRepository transactions;

    public CertificateReadController(CertificateRepository certificates, BlockchainTransactionRepository transactions) {
        this.certificates = certificates;
        this.transactions = transactions;
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public CertificateResponse get(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        Certificate certificate = certificates.findByIdAndOrganizationId(id, principal.organizationId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificate not found"));
        var all = new ArrayList<BlockchainTransaction>();
        all.addAll(transactions.findByCertificateIdAndTransactionTypeOrderByCreatedAtDesc(id, BlockchainTransactionType.ISSUE));
        all.addAll(transactions.findByCertificateIdAndTransactionTypeOrderByCreatedAtDesc(id, BlockchainTransactionType.REVOKE));
        return CertificateMapper.toResponse(certificate, all);
    }
}
