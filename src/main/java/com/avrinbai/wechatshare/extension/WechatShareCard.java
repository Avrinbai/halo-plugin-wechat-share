package com.avrinbai.wechatshare.extension;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
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

        @Schema(maxLength = 16)
        private String cardKind;

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 128)
        private String title;

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 2048)
        private String description;

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 2048)
        private String img;

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 2048)
        private String redirectUrl;

        @Schema(maxLength = 2048)
        private String mediaUrl;

        @Schema(maxLength = 128)
        private String displayName;

        @Schema(maxLength = 64)
        private String optionalLinkLabel;

        @Schema(maxLength = 2048)
        private String optionalLinkUrl;

        private List<FileNote> fileNotes;

        @Schema(maxLength = 512)
        private String contactInfo;

        @Schema(maxLength = 128)
        private String videoTitle;

        @Schema(maxLength = 512)
        private String videoGuideText;

        @Schema(maxLength = 2048)
        private String videoExtraLink;

        @Schema(maxLength = 64)
        private String videoExtraLinkLabel;

        @Schema(maxLength = 128)
        private String videoPasswordHash;

        @Schema(description = "分享链接二维码图片 Base64（不含 data: 前缀），创建时抓取上游接口并写入，避免列表页重复请求")
        private String shareQrcodeBase64;

        @Schema(description = "二维码图片 MIME，例如 image/png")
        private String shareQrcodeMimeType;

        @Schema(description = "累计访问次数（落地页与跳转均计入；异步递增）")
        private Long visitCount;

        
        private Boolean enabled;
    }

    @Data
    @Schema(name = "WechatShareFileNote")
    public static class FileNote {

        @Schema(maxLength = 128)
        private String title;

        @Schema(maxLength = 512)
        private String detail;

        @Schema(description = "true：点击跳转 url；false：仅展示标题与说明文案")
        private Boolean jumpLink;

        @Schema(maxLength = 2048)
        private String url;
    }
}
