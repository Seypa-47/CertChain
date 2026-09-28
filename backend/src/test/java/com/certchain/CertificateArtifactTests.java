package com.certchain;

import com.certchain.artifact.*;
import com.certchain.certificate.*;
import com.certchain.organization.Organization;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CertificateArtifactTests {
    private static final String PUBLIC_ID = "CERT-2026-000001";
    @TempDir Path temporary;

    private Certificate certificate(String name, String program, LocalDate expiry) {
        Organization org = new Organization("Example Training Institute", "private@example.com");
        Certificate certificate = new Certificate(PUBLIC_ID, org, name, "private@example.com",
            program, LocalDate.of(2026, 9, 20));
        ReflectionTestUtils.setField(certificate, "id", UUID.randomUUID());
        certificate.setExpiryDate(expiry);
        certificate.beginIssuance("a".repeat(64), "v1");
        certificate.markIssued(Instant.parse("2026-09-20T08:00:00Z"));
        return certificate;
    }

    @Test void qrDecodesToExactCanonicalVerificationUrl() throws Exception {
        var urls = new VerificationUrlFactory("https://certchain.example");
        byte[] png = new VerificationQrService(urls).png(PUBLIC_ID);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        var bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        assertEquals("https://certchain.example/verify/" + PUBLIC_ID,
            new MultiFormatReader().decode(bitmap).getText());
        assertThrows(IllegalArgumentException.class, () -> urls.forCertificate("../private"));
    }

    @Test void pdfContainsFieldsAndRendersLongTextWithoutExtraPages() throws Exception {
        var urls = new VerificationUrlFactory("https://certchain.example");
        var renderer = new CertificatePdfRenderer(new VerificationQrService(urls), urls);
        String longName = "Alexandra ".repeat(16).trim();
        String longProgram = "Advanced Certificate in Secure Distributed Systems and Applied Blockchain ".repeat(4).trim();
        Certificate sample = certificate(longName, longProgram, LocalDate.of(2028, 9, 20));
        sample.getOrganization().setLogoUrl(sampleLogo());
        byte[] pdf = renderer.render(sample);
        try (var document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document).replaceAll("\\s+", " ");
            assertEquals(1, document.getNumberOfPages());
            assertTrue(text.contains("CERTIFICATE OF ACHIEVEMENT"));
            assertTrue(text.contains(PUBLIC_ID));
            assertTrue(text.contains("Example Training Institute"));
            assertTrue(text.contains("20 September 2026"));
            assertTrue(text.contains("20 September 2028"));
            assertTrue(text.contains("https://certchain.example/verify/" + PUBLIC_ID));
            assertTrue(text.contains("Alexandra"));
            assertTrue(text.contains("Advanced Certificate"));
            BufferedImage rendered = new PDFRenderer(document).renderImageWithDPI(0, 144);
            assertTrue(rendered.getWidth() > 1000);
            assertTrue(rendered.getHeight() > 1500);
            if (Boolean.getBoolean("certchain.writeSample")) {
                Path screenshots = Path.of("../docs/screenshots");
                Files.createDirectories(screenshots);
                Files.write(screenshots.resolve("sample-certificate.pdf"), pdf);
                ImageIO.write(rendered, "PNG", screenshots.resolve("sample-certificate.png").toFile());
            }
        }
        byte[] withoutExpiry = renderer.render(certificate("Jordan Example", "Blockchain Fundamentals", null));
        try (var document = Loader.loadPDF(withoutExpiry)) {
            assertFalse(new PDFTextStripper().getText(document).contains("EXPIRES"));
            if (Boolean.getBoolean("certchain.writeSample")) {
                Path screenshots = Path.of("../docs/screenshots");
                Files.write(screenshots.resolve("sample-no-expiry.pdf"), withoutExpiry);
                ImageIO.write(new PDFRenderer(document).renderImageWithDPI(0, 144), "PNG",
                    screenshots.resolve("sample-no-expiry.png").toFile());
            }
        }
        assertTrue(renderer.render(certificate("W".repeat(200), "W".repeat(300), null)).length > 1000);
    }

    @Test void validEmbeddedLogoAndBadLogoBothProduceReadablePdf() throws Exception {
        Certificate certificate = certificate("Jordan Example", "Course", null);
        certificate.getOrganization().setLogoUrl(sampleLogo());
        var urls = new VerificationUrlFactory("https://certchain.example");
        var renderer = new CertificatePdfRenderer(new VerificationQrService(urls), urls);
        assertTrue(renderer.render(certificate).length > 1000);
        certificate.getOrganization().setLogoUrl("http://127.0.0.1/private");
        assertTrue(renderer.render(certificate).length > 1000);
        assertTrue(renderer.render(certificate("សុខា", "បច្ចេកវិទ្យា Blockchain", null)).length > 1000);
    }

    private static String sampleLogo() throws Exception {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(new java.awt.Color(16, 92, 91));
        graphics.fillRect(0, 0, 64, 64);
        graphics.setColor(java.awt.Color.WHITE);
        graphics.fillOval(15, 15, 34, 34);
        graphics.dispose();
        java.io.ByteArrayOutputStream png = new java.io.ByteArrayOutputStream();
        ImageIO.write(image, "PNG", png);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(png.toByteArray());
    }

    @Test void storageRejectsTraversalAndRoundTripsOnlyExpectedKeys() {
        var storage = new LocalArtifactStorage(temporary.toString());
        String key = "certificates/" + UUID.randomUUID() + "/certificate.pdf";
        storage.write(key, new byte[] {1, 2, 3});
        assertArrayEquals(new byte[] {1, 2, 3}, storage.read(key));
        assertTrue(storage.exists(key));
        assertThrows(IllegalArgumentException.class, () -> storage.write("../secret.pdf", new byte[] {1}));
        assertThrows(IllegalArgumentException.class, () -> storage.read("certificates/../../secret.pdf"));
        assertThrows(IllegalArgumentException.class, () -> storage.read("C:\\Windows\\secret.pdf"));
    }

    @Test void failedArtifactCanRetryWithoutAnyBlockchainSubmission() {
        Certificate certificate = certificate("Jordan Example", "Course", null);
        var repo = mock(CertificateRepository.class);
        var renderer = mock(CertificatePdfRenderer.class);
        var storage = mock(ArtifactStorage.class);
        UUID org = UUID.randomUUID();
        when(repo.lockByIdAndOrganizationId(certificate.getId(), org)).thenReturn(Optional.of(certificate));
        when(repo.findByIdAndOrganizationId(certificate.getId(), org)).thenReturn(Optional.of(certificate));
        when(renderer.render(certificate)).thenThrow(new IllegalStateException("temporary disk error"))
            .thenReturn(new byte[] {1, 2, 3});
        var service = new CertificateArtifactService(repo, storage, renderer);
        assertEquals(CertificateArtifactService.ArtifactState.FAILED,
            service.ensure(certificate.getId(), org).status());
        assertEquals(CertificateLifecycle.ISSUED, certificate.getLifecycle());
        assertEquals(CertificateArtifactService.ArtifactState.READY,
            service.ensure(certificate.getId(), org).status());
        assertNotNull(certificate.getPdfStorageKey());
        verify(renderer, times(2)).render(certificate);
        when(storage.exists(certificate.getPdfStorageKey())).thenReturn(true);
        service.ensure(certificate.getId(), org);
        verify(renderer, times(2)).render(certificate);
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
            () -> service.ensure(certificate.getId(), UUID.randomUUID()));
    }
}
