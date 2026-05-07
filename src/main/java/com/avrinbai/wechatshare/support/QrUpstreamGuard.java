package com.avrinbai.wechatshare.support;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

public final class QrUpstreamGuard {

    private QrUpstreamGuard() {
    }

    public static boolean allows(URI uri) {
        if (uri == null) {
            return false;
        }
        var scheme = uri.getScheme();
        if (scheme == null) {
            return false;
        }
        if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
            return false;
        }
        var host = uri.getHost();
        if (host == null || host.isBlank()) {
            return false;
        }
        try {
            var addr = InetAddress.getByName(host);
            if (addr.isLoopbackAddress()
                || addr.isAnyLocalAddress()
                || addr.isLinkLocalAddress()
                || addr.isSiteLocalAddress()
                || addr.isMulticastAddress()) {
                return false;
            }
        } catch (UnknownHostException e) {
            return false;
        }
        return true;
    }
}
