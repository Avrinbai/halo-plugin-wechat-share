package com.avrinbai.wechatshare.support;

public final class PublicUrls {
    private PublicUrls() {
    }

    public static String absoluteHttp(String siteUrl, String pathAndQuery) {
        if (siteUrl == null || siteUrl.isBlank()) {
            return pathAndQuery.startsWith("/") ? pathAndQuery : "/" + pathAndQuery;
        }
        var base = siteUrl.endsWith("/") ? siteUrl.substring(0, siteUrl.length() - 1) : siteUrl.trim();
        var p = pathAndQuery.startsWith("/") ? pathAndQuery : "/" + pathAndQuery;
        return base + p;
    }
}
