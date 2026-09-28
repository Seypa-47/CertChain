package com.certchain.certificate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Hash;

@Service
public final class CertificateHashService {
    public static final String VERSION = "v1";

    public String canonicalize(String certificateId, String recipientName, String programName,
                               UUID organizationId, LocalDate issueDate, LocalDate expiryDate) {
        Objects.requireNonNull(organizationId);
        Objects.requireNonNull(issueDate);
        return VERSION
            + "|certificateId=" + escape(normalize(certificateId).toUpperCase(Locale.ROOT))
            + "|recipientName=" + escape(normalize(recipientName))
            + "|programName=" + escape(normalize(programName))
            + "|organizationId=" + organizationId
            + "|issueDate=" + issueDate
            + "|expiryDate=" + (expiryDate == null ? "" : expiryDate);
    }

    public String hash(Certificate certificate) {
        return hash(canonicalize(certificate.getCertificateId(), certificate.getRecipientName(),
            certificate.getProgramName(), certificate.getOrganization().getId(),
            certificate.getIssueDate(), certificate.getExpiryDate()));
    }

    public String contractKey(String certificateId) {
        return Hash.sha3String(normalize(certificateId).toUpperCase(Locale.ROOT));
    }

    public String hash(String canonicalData) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(canonicalData.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static String normalize(String input) {
        String value = Normalizer.normalize(Objects.requireNonNull(input), Normalizer.Form.NFC);
        StringBuilder result = new StringBuilder();
        boolean pendingSpace = false;
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)) {
                pendingSpace = result.length() > 0;
            } else {
                if (pendingSpace) result.append(' ');
                result.appendCodePoint(codePoint);
                pendingSpace = false;
            }
        }
        return result.toString();
    }

    private static String escape(String input) {
        return input.replace("\\", "\\\\").replace("|", "\\|").replace("=", "\\=");
    }
}
