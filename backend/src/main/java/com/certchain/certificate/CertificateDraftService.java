package com.certchain.certificate;

import com.certchain.certificate.dto.CertificateListItem;
import com.certchain.certificate.dto.CertificateMapper;
import com.certchain.certificate.dto.CertificatePage;
import com.certchain.certificate.dto.CertificateResponse;
import com.certchain.certificate.dto.CreateCertificateRequest;
import com.certchain.certificate.dto.UpdateCertificateRequest;
import com.certchain.organization.Organization;
import com.certchain.organization.OrganizationRepository;
import com.certchain.transaction.BlockchainTransactionRepository;
import com.certchain.transaction.BlockchainTransactionType;
import jakarta.persistence.criteria.Predicate;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CertificateDraftService {
    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "certificateId", "recipientName",
        "programName", "issueDate", "lifecycle");
    private final CertificateRepository certificates;
    private final OrganizationRepository organizations;
    private final BlockchainTransactionRepository transactions;
    private final CertificateIdGenerator ids;

    public CertificateDraftService(CertificateRepository certificates, OrganizationRepository organizations,
                                   BlockchainTransactionRepository transactions, CertificateIdGenerator ids) {
        this.certificates = certificates;
        this.organizations = organizations;
        this.transactions = transactions;
        this.ids = ids;
    }

    @Transactional
    public CertificateResponse create(UUID organizationId, CreateCertificateRequest request) {
        Organization organization = organizations.findById(organizationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
        Certificate draft = CertificateMapper.toDraft(new CreateCertificateRequest(
            normalizedText(request.recipientName()), normalizedEmail(request.recipientEmail()),
            normalizedText(request.programName()), normalizedDescription(request.description()),
            request.issueDate(), request.expiryDate()), organization, ids.nextId());
        return CertificateMapper.toResponse(certificates.saveAndFlush(draft), List.of());
    }

    @Transactional(readOnly = true)
    public CertificatePage list(UUID organizationId, int page, int size, String query,
                                CertificateLifecycle lifecycle, String sort, String direction) {
        if (page < 0 || size < 1 || size > 100 || !SORT_FIELDS.contains(sort)
            || !("asc".equalsIgnoreCase(direction) || "desc".equalsIgnoreCase(direction))
            || query != null && query.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid certificate list parameters");
        }
        Sort order = Sort.by("asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC,
            sort).and(Sort.by("id"));
        String needle = query == null ? "" : normalizedText(query).toLowerCase(Locale.ROOT);
        Specification<Certificate> tenantFilter = (root, ignored, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("organization").get("id"), organizationId));
            if (lifecycle != null) predicates.add(builder.equal(root.get("lifecycle"), lifecycle));
            if (!needle.isBlank()) {
                String pattern = "%" + needle.replace("\\", "\\\\").replace("%", "\\%")
                    .replace("_", "\\_") + "%";
                predicates.add(builder.or(
                    builder.like(builder.lower(root.get("recipientName")), pattern, '\\'),
                    builder.like(builder.lower(root.get("programName")), pattern, '\\'),
                    builder.like(builder.lower(root.get("certificateId")), pattern, '\\')));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        return CertificatePage.from(certificates.findAll(tenantFilter, PageRequest.of(page, size, order))
            .map(CertificateListItem::from));
    }

    @Transactional(readOnly = true)
    public CertificateResponse get(UUID organizationId, UUID id) {
        Certificate certificate = certificates.findByIdAndOrganizationId(id, organizationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificate not found"));
        var all = new ArrayList<com.certchain.transaction.BlockchainTransaction>();
        all.addAll(transactions.findByCertificateIdAndTransactionTypeOrderByCreatedAtDesc(id, BlockchainTransactionType.ISSUE));
        all.addAll(transactions.findByCertificateIdAndTransactionTypeOrderByCreatedAtDesc(id, BlockchainTransactionType.REVOKE));
        return CertificateMapper.toResponse(certificate, all);
    }

    @Transactional
    public CertificateResponse update(UUID organizationId, UUID id, UpdateCertificateRequest request) {
        Certificate certificate = certificates.lockByIdAndOrganizationId(id, organizationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificate not found"));
        if (certificate.getLifecycle() != CertificateLifecycle.DRAFT) {
            throw new CertificateConflictException("CERTIFICATE_NOT_DRAFT", "Only draft certificates may be edited");
        }
        CertificateMapper.updateDraft(certificate, new UpdateCertificateRequest(
            normalizedText(request.recipientName()), normalizedEmail(request.recipientEmail()),
            normalizedText(request.programName()), normalizedDescription(request.description()),
            request.issueDate(), request.expiryDate()));
        certificates.flush();
        return CertificateMapper.toResponse(certificate, List.of());
    }

    private static String normalizedText(String value) {
        return Normalizer.normalize(value.strip(), Normalizer.Form.NFC).replaceAll("\\s+", " ");
    }

    private static String normalizedEmail(String value) {
        return Normalizer.normalize(value.strip(), Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
    }

    private static String normalizedDescription(String value) {
        return value == null || value.isBlank() ? null : Normalizer.normalize(value.strip(), Normalizer.Form.NFC);
    }
}
