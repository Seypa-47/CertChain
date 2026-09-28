package com.certchain;

import static org.junit.jupiter.api.Assertions.*;

import com.certchain.email.CertificateEmailService.IssuanceEmail;
import com.certchain.email.SmtpCertificateEmailService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/** Explicit local smoke test: run only with MAILPIT_SMOKE=true and a loopback Mailpit server. */
@EnabledIfEnvironmentVariable(named = "MAILPIT_SMOKE", matches = "true")
class MailpitSmokeIT {
    @Test
    void capturesApplicationMultipartEmailAndPdfAttachment() throws Exception {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost("127.0.0.1");
        sender.setPort(1025);
        sender.getJavaMailProperties().put("mail.smtp.connectiontimeout", "5000");
        sender.getJavaMailProperties().put("mail.smtp.timeout", "5000");
        byte[] pdf = Files.readAllBytes(Path.of("../docs/screenshots/sample-certificate.pdf"));
        String publicId = "CERT-2026-123456";
        new SmtpCertificateEmailService(sender, "certificates@certchain.local").send(
            new IssuanceEmail("smoke-recipient@example.test", "Sample Recipient", "Sample Program",
                "Sample Organization", publicId, "http://localhost:3000/verify/" + publicId, pdf));

        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:8025/api/v1/messages"))
            .timeout(Duration.ofSeconds(5)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        var messages = new tools.jackson.databind.ObjectMapper().readTree(response.body()).get("messages");
        assertTrue(messages.size() > 0);
        String id = messages.get(0).get("ID").asText();
        HttpResponse<String> detail = client.send(HttpRequest.newBuilder(
            URI.create("http://127.0.0.1:8025/api/v1/message/" + id)).build(),
            HttpResponse.BodyHandlers.ofString());
        assertEquals(200, detail.statusCode());
        assertTrue(detail.body().contains(publicId));
        assertTrue(detail.body().contains("smoke-recipient@example.test"));
        assertTrue(detail.body().contains("http://localhost:3000/verify/" + publicId));
        assertTrue(detail.body().contains(publicId + ".pdf"));
    }
}
