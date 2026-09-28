package com.certchain.certificate;

public class CertificateConflictException extends RuntimeException {
    private final String code;

    public CertificateConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() { return code; }
}
