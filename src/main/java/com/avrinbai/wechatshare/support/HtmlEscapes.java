package com.avrinbai.wechatshare.support;

import org.springframework.web.util.HtmlUtils;

/**
 * 文本嵌入 HTML 时的统一转义（委托 Spring {@link HtmlUtils#htmlEscape(String)}，与落地页渲染一致）。
 */
public final class HtmlEscapes {

    private HtmlEscapes() {
    }

    public static String text(String raw) {
        return HtmlUtils.htmlEscape(raw == null ? "" : raw);
    }
}
