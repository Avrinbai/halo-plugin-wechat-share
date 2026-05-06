package com.avrinbai.wechatshare.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** 落地页 CSS 位于 classpath；变更 {@code wechat-share/landing/*.css} 后需同步更新下方 SHA-256 期望值。 */
class ShareLandingCssSnapshotTest {

    private static final Map<String, String> EXPECTED_SHA256 = Map.ofEntries(
        Map.entry("audio.css", "42e291f560b405367b2656f2598d03df81826f00a2fd65e2827d9520876f75f6"),
        Map.entry("file.css", "592fba695f4324af072f1a98e9bb40381658792287ff8c6608c8a12a2bf0c288"),
        Map.entry("image.css", "17a2e8b79bbf8de31f7f1c8116b07a73e2d8ac3b467d689d6fe7a9b736947c6e"),
        Map.entry("link.css", "56a1581231867510c419b5b06ece2cb90ea96cace515bb64b51b5d82e75e25cc"),
        Map.entry("shared-banner.css", "2caa0c53db18ecf1bf99e49c456c5f4765f654b07020d117d1823935345f22da"),
        Map.entry("video.css", "d5f59ce8a484d4f39520e4966d3e19b1264cdc841850b2d9f3731fdb4a20319b")
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
