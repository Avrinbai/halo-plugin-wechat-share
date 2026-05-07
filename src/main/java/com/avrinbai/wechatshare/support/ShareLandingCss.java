package com.avrinbai.wechatshare.support;

import com.avrinbai.wechatshare.WechatShareCardKind;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ShareLandingCss {

    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    private ShareLandingCss() {
    }

    public static void appendKind(StringBuilder sb, String cardKind) {
        sb.append(css(switch (WechatShareCardKind.normalize(cardKind)) {
            case WechatShareCardKind.IMAGE -> "image";
            case WechatShareCardKind.AUDIO -> "audio";
            case WechatShareCardKind.VIDEO -> "video";
            case WechatShareCardKind.FILE -> "file";
            default -> "link";
        }));
    }

    public static void appendSharedBanner(StringBuilder sb) {
        sb.append(css("shared-banner"));
    }

    public static void appendHarmonySans(StringBuilder sb) {
        sb.append(css("harmony-sans"));
    }

    private static String css(String resourceStem) {
        return CACHE.computeIfAbsent(resourceStem, ShareLandingCss::read);
    }

    private static String read(String stem) {
        var path = "/wechat-share/landing/" + stem + ".css";
        try (InputStream in = ShareLandingCss.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: wechat-share/landing/" + stem + ".css");
            }
            return normalizeLineEnds(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + path, e);
        }
    }

    private static String normalizeLineEnds(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }

    static byte[] landingCssSnapshotUtf8Bytes(String fileName) {
        if (!fileName.endsWith(".css")) {
            throw new IllegalArgumentException("Expected *.css, got: " + fileName);
        }
        var stem = fileName.substring(0, fileName.length() - ".css".length());
        return css(stem).getBytes(StandardCharsets.UTF_8);
    }
}
