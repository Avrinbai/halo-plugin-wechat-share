package com.avrinbai.wechatshare.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class VisitEnvKindTest {

    @Test
    void classifiesMobileQq() {
        var ua =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15"
                + " (KHTML, like Gecko) Mobile/15E148 QQ/8.4.8";
        assertEquals(VisitEnvKind.QQ, VisitEnvKind.classify(ua));
    }

    @Test
    void wechatTakesPrecedenceOverQqToken() {
        var ua = "Mozilla/5.0 MicroMessenger/8.0.0 QQ/1.0";
        assertEquals(VisitEnvKind.WECHAT, VisitEnvKind.classify(ua));
    }
}
