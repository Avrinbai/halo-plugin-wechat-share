package com.avrinbai.wechatshare.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.web.util.HtmlUtils;

class HtmlEscapesTest {

    @Test
    void matchesSpringHtmlUtilsForTextContext() {
        var raw = "&<>\"'中文";
        assertEquals(HtmlUtils.htmlEscape(raw), HtmlEscapes.text(raw));
    }

    @Test
    void nullSafe() {
        assertEquals("", HtmlEscapes.text(null));
    }
}
