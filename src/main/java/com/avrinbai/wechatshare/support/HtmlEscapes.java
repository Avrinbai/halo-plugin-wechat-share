package com.avrinbai.wechatshare.support;

import org.springframework.web.util.HtmlUtils;

public final class HtmlEscapes {

    private HtmlEscapes() {
    }

    public static String text(String raw) {
        return HtmlUtils.htmlEscape(raw == null ? "" : raw);
    }
}
