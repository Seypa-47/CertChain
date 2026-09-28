package com.certchain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.certchain.email.CertificateEmailService.IssuanceEmail;
import com.certchain.email.SmtpCertificateEmailService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;

class SmtpCertificateEmailServiceTests {
    @Test
    void buildsMultipartTextHtmlAndPdfWithoutTrustingHtmlFields() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage((Session) null));
        var service = new SmtpCertificateEmailService(sender, "certificates@example.test");
        service.send(new IssuanceEmail("recipient@example.test", "Alice <script>", "Security & Safety",
            "Example Academy", "CERT-2026-000001",
            "https://certchain.example/verify/CERT-2026-000001", "%PDF-test".getBytes(StandardCharsets.UTF_8)));

        ArgumentCaptor<MimeMessage> captured = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(captured.capture());
        MimeMessage message = captured.getValue();
        assertEquals("recipient@example.test", message.getAllRecipients()[0].toString());
        assertEquals("certificates@example.test", message.getFrom()[0].toString());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        message.writeTo(output);
        String raw = output.toString(StandardCharsets.UTF_8);
        assertTrue(raw.contains("multipart/mixed"));
        assertTrue(raw.contains("text/plain"));
        assertTrue(raw.contains("text/html"));
        assertTrue(raw.contains("CERT-2026-000001.pdf"));
        assertTrue(raw.contains("Alice &lt;script&gt;"));
    }
}
