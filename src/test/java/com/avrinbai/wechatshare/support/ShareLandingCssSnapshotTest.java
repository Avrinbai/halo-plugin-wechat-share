package com.avrinbai.wechatshare.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** 落地页 CSS 位于 classpath（含 {@code harmony-sans.css}）；变更 {@code wechat-share/landing/*.css} 后需同步更新下方 SHA-256 期望值。 */
class ShareLandingCssSnapshotTest {

    private static final Map<String, String> EXPECTED_SHA256 = Map.ofEntries(
        Map.entry("audio.css", "f5c3062ec242bf133fc4f8b235fe0418cb672298bf24896ae9b802f141b1cca0"),
        Map.entry("file.css", "1a64ce82f20e9eb8cef7705a309b3642670ddec339b26e698c023a9c0e7292eb"),
        Map.entry("harmony-sans.css", "4dc6a72ca33851971426b53b1082df7419f0e9db69db2919ef9575f2668124a9"),
        Map.entry("image.css", "b103a53ecacc4a594f6d355aa383303fc669e3a7956e47a6122895e14695b0cb"),
        Map.entry("link.css", "f2e92f266a51883b6cf9cc92b0166085cef43677e5ef72b2d820db97500d0378"),
        Map.entry("shared-banner.css", "8397e255cb5ab3e3d038e3f4c51a7be1d6ef56dadc7df751ec2b4eae3dd7e6e4"),
        Map.entry("video.css", "13a414c3ce2bf931dc88f8c0562f1c37f9fd746202c93d824ab0c1a4b8871778")
    );

    @Test
    void landingCssSha256MatchesGolden() throws Exception {
        var md = MessageDigest.getInstance("SHA-256");
        for (var e : EXPECTED_SHA256.entrySet()) {
            var path = "/wechat-share/landing/" + e.getKey();
            try (InputStream in = ShareLandingCss.class.getResourceAsStream(path)) {
                if (in == null) {
                    throw new AssertionError("missing resource " + path);
                }
                var bytes = in.readAllBytes();
                var hex = HexFormat.of().formatHex(md.digest(bytes));
                assertEquals(e.getValue(), hex, path);
            }
        }
    }

}
