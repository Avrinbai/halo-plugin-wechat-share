package com.avrinbai.wechatshare.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
public class ShareQrCodeService {

    private static final Logger log = LoggerFactory.getLogger(ShareQrCodeService.class);

    private static final int IMAGE_SIZE_PX = 256;
    private static final int MAX_BYTES = 512 * 1024;
    private static final String MIME_PNG = "image/png";

    /**
     * @param shareUrlToEncode 通常为绝对 http(s) 分享页地址
     */
    public Optional<QrPngResult> encodeShareUrlToPngBase64(String shareUrlToEncode) {
        if (shareUrlToEncode == null || shareUrlToEncode.isBlank()) {
            return Optional.empty();
        }
        var text = shareUrlToEncode.trim();
        try {
            var hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            hints.put(EncodeHintType.MARGIN, 1);

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, IMAGE_SIZE_PX, IMAGE_SIZE_PX, hints);
            var baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
            var body = baos.toByteArray();
            if (body.length == 0 || body.length > MAX_BYTES) {
                return Optional.empty();
            }
            var b64 = Base64.getEncoder().encodeToString(body);
            return Optional.of(new QrPngResult(b64, MIME_PNG));
        } catch (WriterException | IllegalArgumentException ex) {
            log.warn("QR encode failed: {}", ex.toString());
            return Optional.empty();
        } catch (IOException ex) {
            log.warn("QR PNG write failed: {}", ex.toString());
            return Optional.empty();
        }
    }

    public record QrPngResult(String base64, String mimeType) {
    }
}
