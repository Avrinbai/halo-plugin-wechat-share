package com.avrinbai.wechatshare.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;

class WechatShareSiteHandlerSignUrlTest {

    private static ServerRequest req(String fullUrl) {
        var r = MockServerHttpRequest.get(fullUrl);
        var exchange = MockServerWebExchange.from(r);
        return ServerRequest.create(exchange, HandlerStrategies.withDefaults().messageReaders());
    }

    @Test
    void buildWxJsSdkSignUrl_rewritesHostUsingExternalSite() {
        var request = req("http://127.0.0.1:8090/wechat-share/share?sid=123456&hint=0");
        assertEquals(
            "https://blog.example.com/wechat-share/share?sid=123456&hint=0",
            WechatShareSiteHandler.buildWxJsSdkSignUrl(request, "https://blog.example.com")
        );
    }

    @Test
    void buildWxJsSdkSignUrl_noQuery() {
        var request = req("http://10.0.0.5/wechat-share/share");
        assertEquals(
            "https://blog.example.com/wechat-share/share",
            WechatShareSiteHandler.buildWxJsSdkSignUrl(request, "https://blog.example.com/")
        );
    }

    @Test
    void buildWxJsSdkSignUrl_fallsBackToRequestUriWhenExternalBlank() {
        var request = req("https://a.com/x?y=1");
        assertEquals("https://a.com/x?y=1", WechatShareSiteHandler.buildWxJsSdkSignUrl(request, ""));
    }
}
