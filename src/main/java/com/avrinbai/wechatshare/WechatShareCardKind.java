package com.avrinbai.wechatshare;

/**
 * 分享卡片类型。历史数据 {@code cardKind} 为空时视为 {@link #LINK}。
 */
public final class WechatShareCardKind {

    private WechatShareCardKind() {
    }

    public static final String LINK = "link";
    public static final String IMAGE = "image";
    public static final String AUDIO = "audio";
    public static final String VIDEO = "video";
    public static final String FILE = "file";

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return LINK;
        }
        var v = raw.trim().toLowerCase();
        return switch (v) {
            case IMAGE, AUDIO, VIDEO, FILE -> v;
            default -> LINK;
        };
    }

    public static boolean isVideo(String kind) {
        return VIDEO.equals(normalize(kind));
    }
}
