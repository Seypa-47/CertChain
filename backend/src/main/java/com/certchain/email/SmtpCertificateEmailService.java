package com.certchain.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class SmtpCertificateEmailService implements CertificateEmailService {
    private final JavaMailSender sender;
    private final String from;

    public SmtpCertificateEmailService(JavaMailSender sender, @Value("${app.mail.from}") String from) {
        this.sender = sender;
        this.from = from;
    }

    @Override
    public void send(IssuanceEmail email) {
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(email.recipientEmail());
            helper.setSubject("Your certificate " + email.certificateId());
            String plain = "Hello " + email.recipientName() + ",\n\n"
                + email.organizationName() + " has issued your certificate for " + email.programName() + ".\n"
                + "Certificate ID: " + email.certificateId() + "\n"
                + "Verify: " + email.verificationUrl() + "\n\n"
                + "Your certificate PDF is attached.\n";
            String html = "<html><body style=\"font-family:Arial,sans-serif;color:#193431;line-height:1.6\">"
                + "<div style=\"max-width:600px;margin:auto;padding:32px;border:1px solid #d9e5e2\">"
                + "<p style=\"color:#0f766e;font-weight:bold\">CERTCHAIN</p>"
                + "<h1 style=\"font-size:26px\">Your certificate is ready</h1>"
                + "<p>Hello " + escape(email.recipientName()) + ",</p>"
                + "<p>" + escape(email.organizationName()) + " has issued your certificate for <strong>"
                + escape(email.programName()) + "</strong>.</p>"
                + "<p><strong>Certificate ID:</strong> " + escape(email.certificateId()) + "</p>"
                + "<p><a href=\"" + escape(email.verificationUrl())
                + "\" style=\"color:#0f766e\">Verify your certificate</a></p>"
                + "<p>Your certificate PDF is attached.</p></div></body></html>";
            helper.setText(plain, html);
            helper.addAttachment(email.certificateId() + ".pdf", new ByteArrayResource(email.pdf()), "application/pdf");
            sender.send(message);
        } catch (MessagingException failure) {
            throw new IllegalStateException("Certificate email could not be prepared", failure);
        }
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
