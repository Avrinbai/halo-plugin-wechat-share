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

        /**
         * 卡片类型：link（默认）、image、audio、video、file。为空视为 link，兼容旧数据。
         */
        @Schema(maxLength = 16)
        private String cardKind;

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 128)
        private String title;

        /**
         * 摘要或各类型的介绍文案；link 类型历史上限 32，其它类型允许更长。
         */
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 2048)
        private String description;

        /**
         * 封面图或图片类型主图 URL。
         */
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 2048)
        private String img;

        /**
         * {@code /go} 跳转目标（http/https）。
         */
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 2048)
        private String redirectUrl;

        /** 音频 / 视频 / 文件等媒体直链 */
        @Schema(maxLength = 2048)
        private String mediaUrl;

        /** 资源展示名称：图片名 / 音频名 / 文件名 / 视频标题等 */
        @Schema(maxLength = 128)
        private String displayName;

        @Schema(maxLength = 64)
        private String optionalLinkLabel;

        @Schema(maxLength = 2048)
        private String optionalLinkUrl;

        /**
         * 文件卡片：多条相关说明；支持仅文案或跳转链接。非 file 类型应为空。
         */
        private List<FileNote> fileNotes;

        @Schema(maxLength = 512)
        private String contactInfo;

        @Schema(maxLength = 128)
        private String videoTitle;

        @Schema(maxLength = 512)
        private String videoGuideText;

        @Schema(maxLength = 2048)
        private String videoExtraLink;

        /** 视频附加链接在落地页胶囊上展示的文案；无附加链接时应为空 */
        @Schema(maxLength = 64)
        private String videoExtraLinkLabel;

        /** 历史字段：曾用于视频访问密码，现已不再校验；保存卡片时会清空。 */
        @Schema(maxLength = 128)
        private String videoPasswordHash;

        @Schema(description = "分享链接二维码图片 Base64（不含 data: 前缀），创建时抓取上游接口并写入，避免列表页重复请求")
        private String shareQrcodeBase64;

        @Schema(description = "二维码图片 MIME，例如 image/png")
        private String shareQrcodeMimeType;
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
