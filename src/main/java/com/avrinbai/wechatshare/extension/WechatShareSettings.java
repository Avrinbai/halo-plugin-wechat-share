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
    kind = "WechatShareSettings",
    plural = "wechatsharesettings",
    singular = "wechatsharesetting"
)
public class WechatShareSettings extends AbstractExtension {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Spec spec;

    @Data
    @Schema(name = "WechatShareSettingsSpec")
    public static class Spec {
        @Schema(description = "公众号 AppId")
        private String wxAppId;

        @Schema(description = "公众号 AppSecret（请妥善保管）")
        private String wxAppSecret;

        @Schema(description = "已废弃：分享链接统一使用 Halo 外部访问地址，该字段始终为空。")
        private String publicSiteUrl;

        @Schema(description = "公开访问路径前缀（默认 /wechat-share）")
        private String publicBasePath;

        @Schema(description = "二维码 PNG 上游接口（默认官方示例网关）")
        private String qrcodeApiBase;
    }
}
