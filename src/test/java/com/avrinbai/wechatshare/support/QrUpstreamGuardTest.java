package com.avrinbai.wechatshare.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import org.junit.jupiter.api.Test;

class QrUpstreamGuardTest {

    @Test
    void allowsPublicHttps() throws Exception {
        assertTrue(QrUpstreamGuard.allows(URI.create("https://example.com/qr?text=1")));
    }

    @Test
    void rejectsLoopback() throws Exception {
        assertFalse(QrUpstreamGuard.allows(URI.create("http://127.0.0.1:8080/qr")));
    }

    @Test
    void rejectsNonHttpScheme() throws Exception {
        assertFalse(QrUpstreamGuard.allows(URI.create("ftp://example.com/x")));
    }
}
