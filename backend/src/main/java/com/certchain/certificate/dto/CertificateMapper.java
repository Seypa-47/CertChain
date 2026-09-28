package com.certchain.certificate.dto;

import com.certchain.certificate.Certificate;
import com.certchain.certificate.CertificateLifecycle;
import com.certchain.organization.Organization;
import com.certchain.organization.dto.OrganizationSummary;
import com.certchain.transaction.BlockchainTransaction;
import com.certchain.transaction.dto.BlockchainTransactionDto;
import java.util.List;

public final class CertificateMapper {
    private CertificateMapper() {}

    public static Certificate toDraft(CreateCertificateRequest request, Organization organization, String certificateId) {
        Certificate certificate = new Certificate(certificateId, organization, request.recipientName(),
            request.recipientEmail(), request.programName(), request.issueDate());
        certificate.setDescription(request.description());
        certificate.setExpiryDate(request.expiryDate());
        return certificate;
    }

    public static void updateDraft(Certificate certificate, UpdateCertificateRequest request) {
        if (certificate.getLifecycle() != CertificateLifecycle.DRAFT) {
            throw new IllegalStateException("Only draft certificates may be edited");
        }
        certificate.setRecipientName(request.recipientName());
        certificate.setRecipientEmail(request.recipientEmail());
        certificate.setProgramName(request.programName());
        certificate.setDescription(request.description());
        certificate.setIssueDate(request.issueDate());
        certificate.setExpiryDate(request.expiryDate());
    }

    public static CertificateResponse toResponse(Certificate certificate, List<BlockchainTransaction> transactions) {
        Organization organization = certificate.getOrganization();
        return new CertificateResponse(certificate.getId(), certificate.getCertificateId(),
            new OrganizationSummary(organization.getId(), organization.getName(), organization.getLogoUrl()),
            certificate.getRecipientName(), certificate.getRecipientEmail(), certificate.getProgramName(),
            certificate.getDescription(), certificate.getIssueDate(), certificate.getExpiryDate(),
            certificate.getLifecycle(), certificate.getCertificateHash(), certificate.getIssuedAt(),
            certificate.getRevokedAt(), certificate.getCreatedAt(), certificate.getUpdatedAt(),
            transactions.stream().map(CertificateMapper::toTransactionDto).toList());
    }

    private static BlockchainTransactionDto toTransactionDto(BlockchainTransaction tx) {
        return new BlockchainTransactionDto(tx.getId(), tx.getTransactionType(), tx.getStatus(),
            tx.getTransactionHash(), tx.getNetwork(), tx.getChainId(), tx.getContractAddress(),
            tx.getBlockNumber(), tx.getBlockTimestamp(), tx.getSubmittedAt(), tx.getConfirmedAt());
    }
}
