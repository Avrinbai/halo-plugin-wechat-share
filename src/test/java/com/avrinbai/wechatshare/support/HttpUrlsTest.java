package com.avrinbai.wechatshare.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class HttpUrlsTest {

    @Test
    void normalizeAddsHttpsScheme() {
        assertEquals("https://example.com/x", HttpUrls.normalize("example.com/x"));
    }

    @Test
    void normalizeKeepsExplicitScheme() {
        assertEquals("http://a/b", HttpUrls.normalize("http://a/b"));
        assertEquals("https://a/b", HttpUrls.normalize("https://a/b"));
    }

    @Test
    void normalizeProtocolRelative() {
        assertEquals("https://cdn/x", HttpUrls.normalize("//cdn/x"));
    }

    @Test
    void normalizeOrNullBlank() {
        assertNull(HttpUrls.normalizeOrNull("  "));
        assertNull(HttpUrls.normalizeOrNull(null));
    }
}
