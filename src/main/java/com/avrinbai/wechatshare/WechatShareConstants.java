package com.avrinbai.wechatshare;

/**
 * 插件内共享的路径与默认值，避免魔法字符串分散在各层。
 */
public final class WechatShareConstants {

    private WechatShareConstants() {
    }

    /**
     * 未配置 {@code publicBasePath} 时的站点公开路径前缀（与控制台占位文案一致）。
     */
    public static final String DEFAULT_PUBLIC_BASE_PATH = "/wechat-share";

    /**
     * 插件管理端 REST API 根路径（相对站点根）。
     */
    public static final String ADMIN_API_BASE_PATH = "/apis/plugins/wechat-share/admin";
}
