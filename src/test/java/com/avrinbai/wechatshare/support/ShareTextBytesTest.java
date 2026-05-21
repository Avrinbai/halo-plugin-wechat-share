package com.avrinbai.wechatshare.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ShareTextBytesTest {

    @Test
    void truncatesByUtf8BytesNotCharCount() {
        var text = "中文测试";
        var out = ShareTextBytes.truncateUtf8(text, 9);
        assertTrue(out.getBytes(StandardCharsets.UTF_8).length <= 9);
        assertEquals("中文测", out);
    }

    @Test
    void leavesShortTextUntouched() {
        assertEquals("hello", ShareTextBytes.truncateUtf8("hello", 45));
    }

    @Test
    void nullAndBlankSafe() {
        assertEquals("", ShareTextBytes.truncateUtf8(null, 45));
        assertEquals("", ShareTextBytes.truncateUtf8("   ", 45));
    }
}
