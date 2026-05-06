package com.avrinbai.wechatshare.support;

/**
 * 将用户输入的 URL 规范化为可解析的 http(s) 形式（与历史行为一致：{@code //} 补全为 https，无 scheme 时默认 https）。
 */
public final class HttpUrls {

    private HttpUrls() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        var u = raw.trim();
        if (u.isEmpty()) {
            return "";
        }
        if (u.startsWith("//")) {
            return "https:" + u;
        }
        if (!u.matches("(?i)^https?://.*")) {
            return "https://" + u;
        }
        return u;
    }

    public static String normalizeOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return normalize(raw);
    }
}
