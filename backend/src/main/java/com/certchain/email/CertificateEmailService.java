package com.certchain.email;

public interface CertificateEmailService {
    void send(IssuanceEmail email);

    record IssuanceEmail(String recipientEmail, String recipientName, String programName,
                         String organizationName, String certificateId, String verificationUrl, byte[] pdf) {}
}
