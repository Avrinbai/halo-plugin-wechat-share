package com.avrinbai.wechatshare.support;

import java.net.InetSocketAddress;
import org.springframework.web.reactive.function.server.ServerRequest;

public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    public static String resolve(ServerRequest request) {
        var xff = request.headers().firstHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            var first = xff.split(",")[0].trim();
            if (!first.isEmpty()) {
                return truncate(first, 64);
            }
        }
        var xfRealIp = request.headers().firstHeader("X-Real-IP");
        if (xfRealIp != null && !xfRealIp.isBlank()) {
            return truncate(xfRealIp.trim(), 64);
        }
        return request.remoteAddress().map(InetSocketAddress::getHostString).map(s -> truncate(s, 64)).orElse("");
    }

    public static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        var t = s.trim();
        if (t.length() <= max) {
            return t;
        }
        return t.substring(0, max);
    }
}
