package com.certchain.certificate.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record CertificatePage(List<CertificateListItem> content, int page, int size,
                              long totalElements, int totalPages) {
    public static CertificatePage from(Page<CertificateListItem> result) {
        return new CertificatePage(result.getContent(), result.getNumber(), result.getSize(),
            result.getTotalElements(), result.getTotalPages());
    }
}
