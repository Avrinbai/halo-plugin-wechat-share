package com.avrinbai.wechatshare.support;

public final class SharePageConstants {

    private SharePageConstants() {
    }

    public static final String HTML_LANG_ZH_CN = "zh-CN";

    public static final String WECHAT_JSSDK_SCRIPT_URL = "https://res.wx.qq.com/open/js/jweixin-1.6.0.js";

    /** 手机 QQ Web 桥接脚本（{@code mqq.invoke}），见 open.mobile.qq.com。 */
    public static final String QQ_MQQAPI_SCRIPT_URL = "https://open.mobile.qq.com/sdk/qqapi.js";

    /** {@code setShareInfo} title 最大 UTF-8 字节数。 */
    public static final int QQ_SHARE_TITLE_MAX_BYTES = 45;

    /** {@code setShareInfo} desc 最大 UTF-8 字节数。 */
    public static final int QQ_SHARE_DESC_MAX_BYTES = 60;

    /** {@code setShareInfo} share_url 最大 UTF-8 字节数。 */
    public static final int QQ_SHARE_URL_MAX_BYTES = 120;

    public static final String PRECONNECT_HDSLB = "https://s1.hdslb.com";


    public static final int RENDER_BUFFER_INITIAL_CAPACITY = 14_000;
}
