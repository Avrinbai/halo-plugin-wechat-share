package com.avrinbai.wechatshare.extension;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@GVK(
    group = "wechatshare.plugin.halo.run",
    version = "v1alpha1",
    kind = "WechatShareCard",
    plural = "wechatsharecards",
    singular = "wechatsharecard"
)
public class WechatShareCard extends AbstractExtension {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Spec spec;

    @Data
    @Schema(name = "WechatShareCardSpec")
    public static class Spec {
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 32)
        private String sid;

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 32)
        private String title;

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 32)
        private String description;

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 2048)
        private String img;

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 2048)
        private String redirectUrl;

        @Schema(description = "分享链接二维码图片 Base64（不含 data: 前缀），创建时抓取上游接口并写入，避免列表页重复请求")
        private String shareQrcodeBase64;

        @Schema(description = "二维码图片 MIME，例如 image/png")
        private String shareQrcodeMimeType;
    }
}
