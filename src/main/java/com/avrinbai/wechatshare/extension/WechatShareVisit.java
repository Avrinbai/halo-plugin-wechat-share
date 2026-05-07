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
    kind = "WechatShareVisit",
    plural = "wechatsharevisits",
    singular = "wechatsharevisit"
)
public class WechatShareVisit extends AbstractExtension {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Spec spec;

    @Data
    @Schema(name = "WechatShareVisitSpec")
    public static class Spec {

        @Schema(description = "卡片 SID")
        private String sid;

        @Schema(description = "SHARE 或 GO")
        private String hitType;

        @Schema(description = "客户端 IP（截断）")
        private String clientIp;

        @Schema(description = "User-Agent（截断）")
        private String userAgent;

        @Schema(description = "环境分类，见 VisitEnvKind")
        private String envKind;

        @Schema(description = "访问时间（纪元毫秒），用于排序与索引")
        private Long visitedAt;

        @Schema(description = "卡片类型（link/image/audio/video/file），写入时按卡片归一化，用于列表筛选索引")
        private String cardKind;

        @Schema(description = "实验功能：控制台查询 IP 归属后的展示文案（写入扩展持久化，避免重复请求上游）")
        private String ipLocationText;
    }
}
