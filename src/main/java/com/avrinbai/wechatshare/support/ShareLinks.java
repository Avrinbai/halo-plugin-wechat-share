package com.avrinbai.wechatshare.support;

public final class ShareLinks {

    private ShareLinks() {
    }

    public static String sharePathAndQuery(String normalizedPublicBase, String sid) {
        return ShareRoutePaths.sharePathWithSid(normalizedPublicBase, sid);
    }

    public static String goPathAndQuery(String normalizedPublicBase, String sid) {
        return ShareRoutePaths.goPathWithSid(normalizedPublicBase, sid);
    }
}
