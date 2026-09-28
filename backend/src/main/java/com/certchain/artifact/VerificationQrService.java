package com.certchain.artifact;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class VerificationQrService {
    private final VerificationUrlFactory urls;

    public VerificationQrService(VerificationUrlFactory urls) { this.urls = urls; }

    public byte[] png(String certificateId) {
        try {
            var matrix = new QRCodeWriter().encode(urls.forCertificate(certificateId),
                BarcodeFormat.QR_CODE, 360, 360,
                Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                    EncodeHintType.MARGIN, 3));
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        } catch (WriterException | IOException error) {
            throw new IllegalStateException("Verification QR generation failed", error);
        }
    }
}
