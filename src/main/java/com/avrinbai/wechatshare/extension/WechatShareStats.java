package com.avrinbai.wechatshare.extension;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashMap;
import java.util.Map;
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
    kind = "WechatShareStats",
    plural = "wechatsharestats",
    singular = "wechatsharestat"
)
public class WechatShareStats extends AbstractExtension {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Spec spec;

    @Data
    @Schema(name = "WechatShareStatsSpec")
    public static class Spec {

        @Schema(description = "累计访问次数（与明细日志一并递增，用于看板秒开）")
        private Long totalVisits;

        @Schema(description = "微信内置浏览器")
        private Long wechat;

        @Schema(description = "企业微信")
        private Long wework;

        @Schema(description = "其他移动浏览器")
        private Long mobileOther;

        @Schema(description = "桌面浏览器")
        private Long desktop;

        @Schema(description = "未知/空 UA")
        private Long unknown;

        @Schema(description = "按自然日聚合访问次数，键为 yyyy-MM-dd")
        private Map<String, Long> visitsByDay;

        public Map<String, Long> safeVisitsByDay() {
            if (visitsByDay == null) {
                visitsByDay = new HashMap<>();
            }
            return visitsByDay;
        }
    }
}
