package com.avrinbai.wechatshare.support;

/**
 * 访问环境分类（写入扩展与统计；展示文案由控制台本地化）。
 */
public final class VisitEnvKind {

    public static final String WECHAT = "WECHAT";
    public static final String WEWORK = "WEWORK";
    public static final String MOBILE_OTHER = "MOBILE_OTHER";
    public static final String DESKTOP = "DESKTOP";
    public static final String UNKNOWN = "UNKNOWN";

    private VisitEnvKind() {
    }

    public static String classify(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return UNKNOWN;
        }
        var u = userAgent.toLowerCase();
        if (u.contains("micromessenger")) {
            return WECHAT;
        }
        if (u.contains("wxwork")) {
            return WEWORK;
        }
        boolean mobile =
            u.contains("android") || u.contains("iphone") || u.contains("ipad") || u.contains("mobile");
        return mobile ? MOBILE_OTHER : DESKTOP;
    }
}
