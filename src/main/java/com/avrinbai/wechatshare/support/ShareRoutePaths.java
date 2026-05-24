package com.avrinbai.wechatshare.support;

import com.avrinbai.wechatshare.WechatShareConstants;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public final class ShareRoutePaths {

    public static final String ACTION_SHARE = "share";
    public static final String ACTION_GO = "go";
    public static final String ACTION_VIEW = "view";

    private ShareRoutePaths() {
    }

    public static String sharePathWithSid(String normalizedPublicBase, String sid) {
        return pathWithSid(normalizedPublicBase, ACTION_SHARE, sid);
    }

    public static String goPathWithSid(String normalizedPublicBase, String sid) {
        return pathWithSid(normalizedPublicBase, ACTION_GO, sid);
    }

    public static String viewPathWithSid(String normalizedPublicBase, String sid) {
        return pathWithSid(normalizedPublicBase, ACTION_VIEW, sid);
    }

    /** 兼容旧链接：{@code /prefix/share?sid=} */
    public static String sharePathQueryWithSid(String normalizedPublicBase, String sid) {
        return effectiveBase(normalizedPublicBase) + "/" + ACTION_SHARE + "?sid=" + encodeQuerySid(sid);
    }

    public static String goPathQueryWithSid(String normalizedPublicBase, String sid) {
        return effectiveBase(normalizedPublicBase) + "/" + ACTION_GO + "?sid=" + encodeQuerySid(sid);
    }

    public static boolean matchesShare(String pathWithinApp, String normalizedPublicBase) {
        return matchesAction(pathWithinApp, normalizedPublicBase, ACTION_SHARE);
    }

    public static boolean matchesGo(String pathWithinApp, String normalizedPublicBase) {
        return matchesAction(pathWithinApp, normalizedPublicBase, ACTION_GO);
    }

    public static boolean matchesView(String pathWithinApp, String normalizedPublicBase) {
        return matchesAction(pathWithinApp, normalizedPublicBase, ACTION_VIEW);
    }

    public static String resolveSid(String pathWithinApp, String normalizedPublicBase, String action, String querySid) {
        if (querySid != null && !querySid.isBlank()) {
            return querySid.trim();
        }
        return sidFromPath(pathWithinApp, normalizedPublicBase, action).orElse("");
    }

    public static Optional<String> sidFromPath(String pathWithinApp, String normalizedPublicBase, String action) {
        if (pathWithinApp == null || pathWithinApp.isBlank()) {
            return Optional.empty();
        }
        var prefix = effectiveBase(normalizedPublicBase) + "/" + action + "/";
        if (!pathWithinApp.startsWith(prefix)) {
            return Optional.empty();
        }
        var segment = pathWithinApp.substring(prefix.length());
        if (segment.isBlank() || segment.contains("/")) {
            return Optional.empty();
        }
        try {
            return Optional.of(URLDecoder.decode(segment, StandardCharsets.UTF_8).trim());
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private static boolean matchesAction(String path, String normalizedPublicBase, String action) {
        var base = effectiveBase(normalizedPublicBase);
        var exact = base + "/" + action;
        return path.equals(exact) || path.startsWith(exact + "/");
    }

    private static String pathWithSid(String normalizedPublicBase, String action, String sid) {
        return effectiveBase(normalizedPublicBase) + "/" + action + "/" + encodePathSid(sid);
    }

    private static String effectiveBase(String normalizedPublicBase) {
        if (normalizedPublicBase == null || normalizedPublicBase.isBlank()) {
            return WechatShareConstants.DEFAULT_PUBLIC_BASE_PATH;
        }
        var b = normalizedPublicBase.trim();
        return b.startsWith("/") ? b : "/" + b;
    }

    private static String encodePathSid(String sid) {
        if (sid == null || sid.isBlank()) {
            return "";
        }
        return URLEncoder.encode(sid.trim(), StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String encodeQuerySid(String sid) {
        return URLEncoder.encode(sid == null ? "" : sid, StandardCharsets.UTF_8);
    }
}
