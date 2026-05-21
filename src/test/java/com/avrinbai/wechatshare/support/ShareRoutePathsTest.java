package com.avrinbai.wechatshare.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShareRoutePathsTest {

    @Test
    void sharePathWithSid_usesPathSegment() {
        assertEquals("/wechat-share/share/abc123", ShareRoutePaths.sharePathWithSid("/wechat-share", "abc123"));
    }

    @Test
    void resolveSid_prefersQueryThenPath() {
        var path = "/wechat-share/go/xyz";
        assertEquals("fromQuery", ShareRoutePaths.resolveSid(path, "/wechat-share", "go", "fromQuery"));
        assertEquals("xyz", ShareRoutePaths.resolveSid(path, "/wechat-share", "go", ""));
    }

    @Test
    void matchesSharePathVariants() {
        assertTrue(ShareRoutePaths.matchesShare("/wechat-share/share", "/wechat-share"));
        assertTrue(ShareRoutePaths.matchesShare("/wechat-share/share/abc", "/wechat-share"));
    }
}
