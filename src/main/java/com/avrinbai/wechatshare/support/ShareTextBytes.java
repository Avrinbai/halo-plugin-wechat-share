package com.avrinbai.wechatshare.support;

import java.nio.charset.StandardCharsets;

/**
 * 按 UTF-8 字节长度截断文案（用于手机 QQ {@code setShareInfo} 的 title/desc/share_url 限制）。
 */
public final class ShareTextBytes {

    private ShareTextBytes() {
    }

    public static String truncateUtf8(String text, int maxBytes) {
        if (text == null || maxBytes <= 0) {
            return "";
        }
        var t = text.trim();
        if (t.isEmpty()) {
            return "";
        }
        var bytes = t.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) {
            return t;
        }
        int end = maxBytes;
        while (end > 0 && (bytes[end] & 0xC0) == 0x80) {
            end--;
        }
        return new String(bytes, 0, end, StandardCharsets.UTF_8);
    }
}
