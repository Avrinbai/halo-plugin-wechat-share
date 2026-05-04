package com.avrinbai.wechatshare.support;

import com.avrinbai.wechatshare.WechatShareConstants;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 公开分享页与跳转页的 path + query（sid）。
 */
public final class ShareLinks {

    private ShareLinks() {
    }

    public static String sharePathAndQuery(String normalizedPublicBase, String sid) {
        return pathWithAction(normalizedPublicBase, "share", sid);
    }

    public static String goPathAndQuery(String normalizedPublicBase, String sid) {
        return pathWithAction(normalizedPublicBase, "go", sid);
    }

    private static String pathWithAction(String normalizedPublicBase, String action, String sid) {
        return effectiveBase(normalizedPublicBase) + "/" + action + "?sid=" + encodeSid(sid);
    }

    private static String effectiveBase(String normalizedPublicBase) {
        if (normalizedPublicBase == null || normalizedPublicBase.isBlank()) {
            return WechatShareConstants.DEFAULT_PUBLIC_BASE_PATH;
        }
        var b = normalizedPublicBase.trim();
        return b.startsWith("/") ? b : "/" + b;
    }

    private static String encodeSid(String sid) {
        return URLEncoder.encode(sid == null ? "" : sid, StandardCharsets.UTF_8);
    }
}
