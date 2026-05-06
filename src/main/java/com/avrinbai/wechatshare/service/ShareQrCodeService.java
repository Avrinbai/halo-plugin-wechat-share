package com.avrinbai.wechatshare.service;

import com.avrinbai.wechatshare.support.QrUpstreamGuard;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ShareQrCodeService {

    private static final Logger log = LoggerFactory.getLogger(ShareQrCodeService.class);

    private static final int MAX_BYTES = 512 * 1024;

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();

    public Optional<QrFetchResult> fetchAsBase64(String qrcodeApiBase, String shareUrlToEncode) {
        if (qrcodeApiBase == null || qrcodeApiBase.isBlank()) {
            return Optional.empty();
        }
        if (shareUrlToEncode == null || shareUrlToEncode.isBlank()) {
            return Optional.empty();
        }
        var base = qrcodeApiBase.trim();
        var sep = base.contains("?") ? "&" : "?";
        var url =
            base + sep + "text=" + java.net.URLEncoder.encode(shareUrlToEncode.trim(), StandardCharsets.UTF_8);
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException ex) {
            log.warn("QR upstream URL invalid: {}", ex.toString());
            return Optional.empty();
        }
        if (!QrUpstreamGuard.allows(uri)) {
            log.warn("QR upstream host rejected: {}", uri.getHost());
            return Optional.empty();
        }
        try {
            var req = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(20)).GET().build();
            var res = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (res.statusCode() < 200 || res.statusCode() >= 300) {
                log.warn("QR upstream HTTP {} for {}", res.statusCode(), uri.getHost());
                return Optional.empty();
            }
            var body = res.body();
            if (body == null || body.length == 0 || body.length > MAX_BYTES) {
                return Optional.empty();
            }
            var mimeFromHeader = parseContentType(res.headers().firstValue("content-type").orElse(""));
            var mimeFromMagic = detectMime(body);
            var mime = mimeFromHeader != null ? mimeFromHeader : mimeFromMagic;
            var b64 = Base64.getEncoder().encodeToString(body);
            return Optional.of(new QrFetchResult(b64, mime));
        } catch (Exception ex) {
            log.warn("QR fetch failed: {}", ex.toString());
            return Optional.empty();
        }
    }

    private static String parseContentType(String ct) {
        if (ct == null || ct.isBlank()) {
            return null;
        }
        var part = ct.split(";")[0].trim().toLowerCase(Locale.ROOT);
        if (part.startsWith("image/png")) {
            return "image/png";
        }
        if (part.startsWith("image/jpeg") || part.startsWith("image/jpg")) {
            return "image/jpeg";
        }
        if (part.startsWith("image/gif")) {
            return "image/gif";
        }
        if (part.startsWith("image/webp")) {
            return "image/webp";
        }
        return null;
    }

    private static String detectMime(byte[] b) {
        if (b.length >= 8 && b[0] == (byte) 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "image/png";
        }
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length >= 6 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') {
            return "image/gif";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F') {
            return "image/webp";
        }
        return "image/png";
    }

    public record QrFetchResult(String base64, String mimeType) {
    }
}
