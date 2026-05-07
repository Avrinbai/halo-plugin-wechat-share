package com.avrinbai.wechatshare.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ShareLandingCssSnapshotTest {

    private static final Map<String, String> EXPECTED_SHA256 = Map.ofEntries(
        Map.entry("audio.css", "f5c3062ec242bf133fc4f8b235fe0418cb672298bf24896ae9b802f141b1cca0"),
        Map.entry("file.css", "1a64ce82f20e9eb8cef7705a309b3642670ddec339b26e698c023a9c0e7292eb"),
        Map.entry("harmony-sans.css", "eeadd57b777ecd9e6460842d0e978b61c604d2828749b73e8ffb95108cd0d826"),
        Map.entry("image.css", "b103a53ecacc4a594f6d355aa383303fc669e3a7956e47a6122895e14695b0cb"),
        Map.entry("link.css", "f2e92f266a51883b6cf9cc92b0166085cef43677e5ef72b2d820db97500d0378"),
        Map.entry("shared-banner.css", "91dec7e1ed7462c72f83b721e33654f8091b17a46ca768b07b3db8bee56d8fdc"),
        Map.entry("video.css", "13a414c3ce2bf931dc88f8c0562f1c37f9fd746202c93d824ab0c1a4b8871778")
    );

    @Test
    void landingCssSha256MatchesGolden() throws Exception {
        var md = MessageDigest.getInstance("SHA-256");
        for (var e : EXPECTED_SHA256.entrySet()) {
            var path = "/wechat-share/landing/" + e.getKey();
            var bytes = ShareLandingCss.landingCssSnapshotUtf8Bytes(e.getKey());
            var hex = HexFormat.of().formatHex(md.digest(bytes));
            assertEquals(e.getValue(), hex, path);
        }
    }

}
