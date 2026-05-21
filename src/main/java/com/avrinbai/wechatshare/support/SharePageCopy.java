package com.avrinbai.wechatshare.support;

public final class SharePageCopy {

    private SharePageCopy() {
    }

    public static final String FALLBACK_PAGE_TITLE = "微信分享";

    public static final String SIG_STATIC_HINT_LEAD = "未正确配置公众号信息，";
    public static final String SIG_STATIC_HINT_TAIL = "请到插件「插件配置」填写 AppId、AppSecret。";

    public static final String HINT_SHARE_TOP_RIGHT_EFFECT = "在右上角分享给好友或群，可查看自定义卡片效果（微信 / QQ）";
    public static final String HINT_SHARE_TOP_RIGHT_THEN_HIDES = "在右上角分享给好友或群后，此提示将自动隐藏（微信 / QQ）";

    public static final String LINK_CARD_SVG_LABEL = "链接";

    public static final String IMAGE_HEADLINE_FALLBACK = "图片";

    public static final String AUDIO_TITLE_FALLBACK = "未命名曲目";
    public static final String AUDIO_ARTIST_FALLBACK = "纯音乐，请欣赏";

    public static final String ARIA_PLAY_PAUSE = "播放/暂停";

    public static final String VIDEO_HEADLINE_FALLBACK = "视频";
    public static final String VIDEO_EXTRA_CHIP_DEFAULT = "相关链接";
    public static final String VIDEO_EXPAND = "展开";
    public static final String VIDEO_COLLAPSE = "收起";
    public static final String VIDEO_EMPTY = "暂无视频地址";
    public static final String ARIA_PROGRESS = "进度";

    public static final String FILE_PRIMARY_FALLBACK = "文件下载";
    public static final String FILE_DOWNLOAD = "下载";
    public static final String FILE_DOWNLOAD_TIP = "若微信内拦截下载，可使用右上角菜单「在浏览器打开」。";
    public static final String FILE_NO_URL = "暂无可用的下载地址，请稍后再试。";

    public static final String SECTION_RELATED_NOTES = "相关说明";
    public static final String NOTE_OPEN_IN_BROWSER = "在浏览器中打开";
    public static final String NOTE_LINK_FALLBACK_TITLE = "链接";
    public static final String LEGACY_LINK_TITLE_FALLBACK = "查看详情";

    public static final String WX_SCRIPT_LOAD_FAIL =
        "无法加载微信 JSSDK 脚本，自定义分享不可用。"
            + "请检查网络或稍后重试；若域名/网络拦截了对 res.wx.qq.com 的访问也会导致此问题。";

    public static final String WX_SCRIPT_CONFIG_FAIL =
        "微信 JSSDK 校验失败：请核对插件中的公众号 AppId / AppSecret、"
            + "JS 接口安全域名是否与当前页面域名一致，并确认「外部访问地址」与签名所用链接一致。";

    public static final String WX_SCRIPT_WX_UNDEFINED =
        "微信 JSSDK 已请求但未暴露 wx 对象，无法继续配置分享。请更换微信内置浏览器重试。";

    public static final String QQ_SCRIPT_LOAD_FAIL =
        "无法加载手机 QQ 分享桥接脚本，QQ 内自定义分享可能不可用。请使用手机 QQ 内置浏览器打开本页后重试。";
}
