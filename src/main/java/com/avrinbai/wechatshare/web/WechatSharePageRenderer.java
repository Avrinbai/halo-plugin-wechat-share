package com.avrinbai.wechatshare.web;

import com.avrinbai.wechatshare.extension.WechatShareCard;
import com.avrinbai.wechatshare.extension.WechatShareSettings;
import com.avrinbai.wechatshare.service.WeChatJsBridgeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
public class WechatSharePageRenderer {

    private final ObjectMapper objectMapper;
    private final WeChatJsBridgeService weChatJsBridgeService;

    public WechatSharePageRenderer(ObjectMapper objectMapper, WeChatJsBridgeService weChatJsBridgeService) {
        this.objectMapper = objectMapper;
        this.weChatJsBridgeService = weChatJsBridgeService;
    }

    public String render(WechatShareCard card, WechatShareSettings settings, String signUrl, String wechatShareLink) throws Exception {
        var spec = card.getSpec();
        var title = spec.getTitle() == null ? "" : spec.getTitle();
        var desc = spec.getDescription() == null ? "" : spec.getDescription();
        var img = spec.getImg() == null ? "" : spec.getImg();

        var appId = "";
        var secret = "";
        if (settings != null && settings.getSpec() != null) {
            appId = settings.getSpec().getWxAppId() == null ? "" : settings.getSpec().getWxAppId();
            secret = settings.getSpec().getWxAppSecret() == null ? "" : settings.getSpec().getWxAppSecret();
        }

        var sig = weChatJsBridgeService.sign(appId, secret, signUrl);

        var displayTitle = title.isBlank() ? "微信分享" : title;

        var sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"zh-CN\">\n<head>\n");
        sb.append("<meta charset=\"utf-8\">\n");
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("<title>").append(HtmlUtils.htmlEscape(displayTitle)).append("</title>\n");
        sb.append("<style>\n");
        sb.append(":root{color-scheme:light;--fg:#141414;--muted:#525252;--faint:#737373;--line:#ebebeb;--sheet:#fff;}\n");
        sb.append("*{box-sizing:border-box;}\n");
        sb.append("html{-webkit-text-size-adjust:100%;}\n");
        sb.append("body{margin:0;min-height:100vh;min-height:100dvh;color:var(--fg);background:#f7f7f8;");
        sb.append(
            "font-family:-apple-system,BlinkMacSystemFont,\"Segoe UI\",Roboto,\"Helvetica Neue\",Arial,\"PingFang SC\","
                + "\"Hiragino Sans GB\",\"Microsoft YaHei\",sans-serif;");
        sb.append("line-height:1.5;-webkit-font-smoothing:antialiased;}\n");
        sb.append(".wrap{min-height:100vh;min-height:100dvh;display:flex;align-items:center;justify-content:center;");
        sb.append("padding:max(20px,env(safe-area-inset-top)) max(18px,env(safe-area-inset-right)) ");
        sb.append("max(24px,env(safe-area-inset-bottom)) max(18px,env(safe-area-inset-left));}\n");
        sb.append(".sheet{width:100%;max-width:21.5rem;margin:0 auto;background:var(--sheet);");
        sb.append("border:1px solid var(--line);border-radius:12px;padding:clamp(20px,5vw,26px) clamp(18px,4.5vw,22px);");
        sb.append("}\n");
        sb.append("h1{margin:0 0 .6rem;font-size:clamp(1.125rem,4vw,1.3125rem);font-weight:600;");
        sb.append("letter-spacing:-.015em;line-height:1.35;text-align:center;}\n");
        sb.append(".sub{margin:0 0 1rem;color:var(--muted);font-size:.9375rem;line-height:1.55;");
        sb.append("text-align:center;}\n");
        sb.append(".note{margin:0;padding-top:1rem;border-top:1px solid var(--line);color:var(--faint);");
        sb.append("font-size:.875rem;line-height:1.65;text-align:center;}\n");
        sb.append(".menu{font-weight:600;color:var(--muted);letter-spacing:.06em;}\n");
        sb.append("@media (max-width:380px){.sheet{border-radius:10px;padding:18px 16px;}h1{font-size:1.0625rem;}}\n");
        sb.append("</style>\n</head>\n<body>\n");
        sb.append("<div class=\"wrap\"><main class=\"sheet\">");
        sb.append("<h1>").append(HtmlUtils.htmlEscape(displayTitle)).append("</h1>");
        if (!desc.isBlank()) {
            sb.append("<p class=\"sub\">").append(HtmlUtils.htmlEscape(desc)).append("</p>");
        }
        sb.append("<p class=\"note\">右上角 <span class=\"menu\" aria-label=\"更多\">···</span>，分享至朋友或朋友圈查看效果。</p>");
        sb.append("</main></div>\n");

        if (sig.usable()) {
            Map<String, Object> cfg = new LinkedHashMap<>();
            cfg.put("debug", false);
            cfg.put("appId", sig.appId());
            cfg.put("timestamp", String.valueOf(sig.timestamp()));
            cfg.put("nonceStr", sig.nonceStr());
            cfg.put("signature", sig.signature());
            cfg.put("jsApiList", java.util.List.of("updateAppMessageShareData", "updateTimelineShareData"));

            var cfgJson = objectMapper.writeValueAsString(cfg);

            Map<String, Object> timeline = new LinkedHashMap<>();
            timeline.put("title", title);
            timeline.put("link", wechatShareLink);
            timeline.put("imgUrl", img);

            Map<String, Object> appMsg = new LinkedHashMap<>();
            appMsg.put("title", title);
            appMsg.put("desc", desc);
            appMsg.put("link", wechatShareLink);
            appMsg.put("imgUrl", img);

            var timelineJson = objectMapper.writeValueAsString(timeline);
            var appMsgJson = objectMapper.writeValueAsString(appMsg);

            sb.append("<script src=\"https://res.wx.qq.com/open/js/jweixin-1.6.0.js\"></script>\n<script>\n");
            sb.append("wx.config(").append(cfgJson).append(");\n");
            sb.append("wx.ready(function () {\n");
            sb.append("  wx.updateTimelineShareData(").append(timelineJson).append(");\n");
            sb.append("  wx.updateAppMessageShareData(").append(appMsgJson).append(");\n");
            sb.append("});\n");
            sb.append("wx.error(function (res) { console.log(res); });\n");
            sb.append("</script>\n");
        }

        sb.append("</body>\n</html>\n");

        return sb.toString();
    }
}
