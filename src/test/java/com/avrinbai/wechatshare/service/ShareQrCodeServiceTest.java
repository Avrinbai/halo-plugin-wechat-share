package com.avrinbai.wechatshare.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class ShareQrCodeServiceTest {

    private final ShareQrCodeService service = new ShareQrCodeService();

    @Test
    void encodesHttpUrlToPngBase64() {
        var opt = service.encodeShareUrlToPngBase64("https://example.com/wechat-share/share?sid=123456");
        assertTrue(opt.isPresent());
        var r = opt.get();
        assertTrue(r.base64() != null && !r.base64().isBlank());
        assertTrue(r.mimeType().contains("png"));
        var decoded = Base64.getDecoder().decode(r.base64());
        assertTrue(decoded.length > 32);
        assertTrue(decoded[0] == (byte) 0x89 && decoded[1] == 'P' && decoded[2] == 'N' && decoded[3] == 'G');
    }

    @Test
    void emptyInputYieldsEmpty() {
        assertTrue(service.encodeShareUrlToPngBase64(null).isEmpty());
        assertTrue(service.encodeShareUrlToPngBase64("   ").isEmpty());
    }
}
