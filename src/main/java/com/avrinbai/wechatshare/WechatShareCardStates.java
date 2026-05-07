package com.avrinbai.wechatshare;

import com.avrinbai.wechatshare.extension.WechatShareCard;


public final class WechatShareCardStates {

    private WechatShareCardStates() {
    }

    public static boolean isEnabled(WechatShareCard card) {
        if (card == null || card.getSpec() == null) {
            return false;
        }
        var e = card.getSpec().getEnabled();
        return e == null || Boolean.TRUE.equals(e);
    }
}
