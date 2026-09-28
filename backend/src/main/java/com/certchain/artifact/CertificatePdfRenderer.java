package com.certchain.artifact;

import com.certchain.certificate.Certificate;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

@Service
public class CertificatePdfRenderer {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.ENGLISH);
    private final VerificationQrService qr;
    private final VerificationUrlFactory urls;

    public CertificatePdfRenderer(VerificationQrService qr, VerificationUrlFactory urls) {
        this.qr = qr;
        this.urls = urls;
    }

    public byte[] render(Certificate certificate) {
        try (PDDocument document = new PDDocument()) {
            PDFont regular = font(document, "/fonts/NotoSans-Regular.ttf");
            PDFont bold = font(document, "/fonts/NotoSans-Bold.ttf");
            PDFont khmer = font(document, "/fonts/NotoSansKhmer-Regular.ttf");
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            float width = page.getMediaBox().getWidth();
            float height = page.getMediaBox().getHeight();
            try (PDPageContentStream canvas = new PDPageContentStream(document, page)) {
                canvas.setNonStrokingColor(new Color(250, 249, 245));
                canvas.addRect(0, 0, width, height);
                canvas.fill();
                canvas.setNonStrokingColor(new Color(16, 92, 91));
                canvas.addRect(0, height - 14, width, 14);
                canvas.fill();
                canvas.setStrokingColor(new Color(16, 92, 91));
                canvas.setLineWidth(1.5f);
                canvas.addRect(28, 27, width - 56, height - 56);
                canvas.stroke();

                float logoX = 53;
                PDImageXObject logo = logo(document, certificate.getOrganization().getLogoUrl());
                if (logo != null) {
                    float scale = Math.min(55f / logo.getWidth(), 55f / logo.getHeight());
                    canvas.drawImage(logo, logoX, 736, logo.getWidth() * scale, logo.getHeight() * scale);
                } else {
                    canvas.setNonStrokingColor(new Color(16, 92, 91));
                    canvas.addRect(logoX, 736, 54, 54);
                    canvas.fill();
                    draw(canvas, bold, khmer, 20, 69, 755, "C", 255, 255, 255);
                }
                drawWrapped(canvas, certificate.getOrganization().getName(), bold, khmer,
                    112, 773, 420, 18, 12, 2, 22, 25, 46, 46);
                draw(canvas, regular, khmer, 10, 53, 704,
                    "CERTCHAIN  /  VERIFIED CREDENTIAL", 16, 92, 91);
                draw(canvas, bold, khmer, 29, 53, 659,
                    "CERTIFICATE OF ACHIEVEMENT", 25, 46, 46);
                draw(canvas, regular, khmer, 11, 53, 623, "PRESENTED TO", 97, 109, 111);
                drawWrapped(canvas, certificate.getRecipientName(), bold, khmer,
                    53, 590, 488, 27, 12, 5, 31, 25, 46, 46);
                draw(canvas, regular, khmer, 11, 53, 430, "FOR COMPLETING", 97, 109, 111);
                drawWrapped(canvas, certificate.getProgramName(), regular, khmer,
                    53, 407, 488, 18, 9, 7, 20, 25, 46, 46);

                canvas.setStrokingColor(new Color(201, 213, 210));
                canvas.moveTo(53, 244);
                canvas.lineTo(541, 244);
                canvas.stroke();
                draw(canvas, regular, khmer, 10, 53, 222, "ISSUED", 97, 109, 111);
                draw(canvas, bold, khmer, 13, 53, 202, DATE.format(certificate.getIssueDate()), 25, 46, 46);
                if (certificate.getExpiryDate() != null) {
                    draw(canvas, regular, khmer, 10, 53, 178, "EXPIRES", 97, 109, 111);
                    draw(canvas, bold, khmer, 13, 53, 158, DATE.format(certificate.getExpiryDate()), 25, 46, 46);
                }
                draw(canvas, regular, khmer, 10, 53, 132, "CERTIFICATE ID", 97, 109, 111);
                draw(canvas, bold, khmer, 16, 53, 110, certificate.getCertificateId(), 16, 92, 91);
                draw(canvas, regular, khmer, 9, 53, 77, "Verify this certificate online:", 97, 109, 111);
                drawWrapped(canvas, urls.forCertificate(certificate.getCertificateId()), regular, khmer,
                    53, 59, 325, 9, 8, 2, 13, 16, 92, 91);
                PDImageXObject qrImage = PDImageXObject.createFromByteArray(document,
                    qr.png(certificate.getCertificateId()), "verification-qr");
                canvas.drawImage(qrImage, 397, 70, 142, 142);
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        } catch (IOException error) {
            throw new IllegalStateException("PDF generation failed", error);
        }
    }

    private static PDFont font(PDDocument document, String resource) throws IOException {
        try (InputStream input = CertificatePdfRenderer.class.getResourceAsStream(resource)) {
            if (input == null) throw new IOException("Bundled certificate font missing");
            return PDType0Font.load(document, input);
        }
    }

    private static PDImageXObject logo(PDDocument document, String value) {
        if (value == null || value.length() > 2048
            || !(value.startsWith("data:image/png;base64,") || value.startsWith("data:image/jpeg;base64,"))) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(value.substring(value.indexOf(',') + 1));
            if (bytes.length > 1_000_000) return null;
            try (ImageInputStream input = ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(bytes))) {
                if (input == null) return null;
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) return null;
                var reader = readers.next();
                try {
                    reader.setInput(input);
                    if (reader.getWidth(0) < 1 || reader.getHeight(0) < 1
                        || reader.getWidth(0) > 1000 || reader.getHeight(0) > 1000) return null;
                } finally { reader.dispose(); }
            }
            return PDImageXObject.createFromByteArray(document, bytes, "organization-logo");
        } catch (RuntimeException | IOException invalid) {
            return null;
        }
    }

    private static void drawWrapped(PDPageContentStream canvas, String value, PDFont font, PDFont fallback,
                                    float x, float y, float maxWidth, int initialSize, int minimumSize,
                                    int maxLines, int leading, int red, int green, int blue) throws IOException {
        for (int size = initialSize; size >= minimumSize; size--) {
            List<String> lines = wrap(value, font, fallback, size, maxWidth);
            if (lines.size() <= maxLines) {
                for (String line : lines) {
                    draw(canvas, font, fallback, size, x, y, line, red, green, blue);
                    y -= leading;
                }
                return;
            }
        }
        throw new IllegalArgumentException("Certificate text exceeds available page space");
    }

    private static List<String> wrap(String value, PDFont font, PDFont fallback, int size, float maxWidth)
        throws IOException {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : value.strip().split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (width(candidate, font, fallback, size) <= maxWidth) {
                line.setLength(0);
                line.append(candidate);
                continue;
            }
            if (!line.isEmpty()) {
                lines.add(line.toString());
                line.setLength(0);
            }
            for (int offset = 0; offset < word.length();) {
                int cp = word.codePointAt(offset);
                offset += Character.charCount(cp);
                String fragment = new String(Character.toChars(cp));
                if (!line.isEmpty() && width(line + fragment, font, fallback, size) > maxWidth) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                line.append(fragment);
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }

    private static float width(String text, PDFont font, PDFont fallback, float size) throws IOException {
        float width = 0;
        for (int offset = 0; offset < text.length();) {
            int cp = text.codePointAt(offset);
            offset += Character.charCount(cp);
            PDFont chosen = choose(font, fallback, cp);
            String character = new String(Character.toChars(cp));
            try { width += chosen.getStringWidth(character) * size / 1000; }
            catch (IllegalArgumentException missing) { width += font.getStringWidth("?") * size / 1000; }
        }
        return width;
    }

    private static PDFont choose(PDFont preferred, PDFont fallback, int cp) throws IOException {
        String character = new String(Character.toChars(cp));
        try { preferred.encode(character); return preferred; }
        catch (IllegalArgumentException unsupported) {
            try { fallback.encode(character); return fallback; }
            catch (IllegalArgumentException missing) { return preferred; }
        }
    }

    private static void draw(PDPageContentStream canvas, PDFont font, PDFont fallback, float size,
                             float x, float y, String text, int red, int green, int blue) throws IOException {
        canvas.setNonStrokingColor(new Color(red, green, blue));
        canvas.beginText();
        canvas.newLineAtOffset(x, y);
        for (int offset = 0; offset < text.length();) {
            int cp = text.codePointAt(offset);
            offset += Character.charCount(cp);
            String character = new String(Character.toChars(cp));
            PDFont chosen = choose(font, fallback, cp);
            try { chosen.encode(character); }
            catch (IllegalArgumentException missing) { character = "?"; chosen = font; }
            canvas.setFont(chosen, size);
            canvas.showText(character);
        }
        canvas.endText();
    }
}
