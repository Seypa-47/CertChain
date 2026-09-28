package com.certchain.certificate.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateCertificateRequest(
    @NotBlank @Size(max = 200) String recipientName,
    @NotBlank @Email @Size(max = 320) String recipientEmail,
    @NotBlank @Size(max = 300) String programName,
    @Size(max = 2000) String description,
    @NotNull @PastOrPresent LocalDate issueDate,
    LocalDate expiryDate
) {
    public UpdateCertificateRequest {
        if (recipientEmail != null) recipientEmail = recipientEmail.strip();
    }

    @AssertTrue(message = "expiryDate must be on or after issueDate")
    public boolean isExpiryDateValid() {
        return issueDate == null || expiryDate == null || !expiryDate.isBefore(issueDate);
    }
}
