package com.avrinbai.wechatshare.support;

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
