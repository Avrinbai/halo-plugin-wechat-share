package com.avrinbai.wechatshare.web;

import com.avrinbai.wechatshare.WechatShareCardKind;
import com.avrinbai.wechatshare.extension.WechatShareCard;
import com.avrinbai.wechatshare.extension.WechatShareSettings;
import com.avrinbai.wechatshare.service.WeChatJsBridgeService;
import com.avrinbai.wechatshare.service.WechatShareCardService;
import com.avrinbai.wechatshare.support.HtmlEscapes;
import com.avrinbai.wechatshare.support.ShareLandingCss;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class WechatSharePageRenderer {

    private static final String WECHAT_JSSDK_URL = "https://res.wx.qq.com/open/js/jweixin-1.6.0.js";

    private final ObjectMapper objectMapper;
    private final WeChatJsBridgeService weChatJsBridgeService;

    public WechatSharePageRenderer(ObjectMapper objectMapper, WeChatJsBridgeService weChatJsBridgeService) {
        this.objectMapper = objectMapper;
        this.weChatJsBridgeService = weChatJsBridgeService;
    }

    /**
     * @param showShareHint 
     */
    public String render(
        WechatShareCard card,
        WechatShareSettings settings,
        String signUrl,
        String wechatShareLink,
        boolean showShareHint
    ) throws Exception {
        var spec = card.getSpec();
        var kind = WechatShareCardKind.normalize(spec.getCardKind());

        var shareTitle = resolveShareTitle(kind, spec);
        var shareDesc = resolveShareDesc(kind, spec);
        var shareImg = nz(spec.getImg());

        var appId = "";
        var secret = "";
        if (settings != null && settings.getSpec() != null) {
            appId = settings.getSpec().getWxAppId() == null ? "" : settings.getSpec().getWxAppId();
            secret = settings.getSpec().getWxAppSecret() == null ? "" : settings.getSpec().getWxAppSecret();
        }
        var sig = weChatJsBridgeService.sign(appId, secret, signUrl);

        var displayTitle = shareTitle.isBlank() ? "微信分享" : shareTitle;

        var sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"zh-CN\">\n<head>\n");
        sb.append("<meta charset=\"utf-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("<meta name=\"color-scheme\" content=\"");
        if (WechatShareCardKind.AUDIO.equals(kind) || WechatShareCardKind.VIDEO.equals(kind)) {
            sb.append("dark");
        } else {
            sb.append("light");
        }
        sb.append("\">\n");
        sb.append("<title>").append(HtmlEscapes.text(displayTitle)).append("</title>\n");
        sb.append("<link rel=\"preconnect\" href=\"https://s1.hdslb.com\" crossorigin>\n");
        sb.append("<style>\n");
        appendStylesForKind(sb, kind);
        sb.append("</style></head><body");

        if (WechatShareCardKind.VIDEO.equals(kind)) {
            sb.append(" class=\"body-v\"");
        } else if (WechatShareCardKind.FILE.equals(kind)) {
            sb.append(" class=\"body-f\"");
        } else if (WechatShareCardKind.IMAGE.equals(kind)) {
            sb.append(" class=\"body-im\"");
        } else if (WechatShareCardKind.AUDIO.equals(kind)) {
            sb.append(" class=\"body-au\"");
        }
        sb.append(">\n");

        appendShellForKind(sb, kind, spec, sig, showShareHint);

        if (sig.usable()) {
            appendWxScripts(sb, sig, shareTitle, shareDesc, shareImg, wechatShareLink);
        }

        if (WechatShareCardKind.AUDIO.equals(kind)) {
            appendAudioUiScript(sb, spec);
        }
        if (WechatShareCardKind.VIDEO.equals(kind)) {
            appendVideoUiScript(sb);
        }

        sb.append("</body>\n</html>\n");
        return sb.toString();
    }

    private static String resolveShareTitle(String kind, WechatShareCard.Spec spec) {
        if (WechatShareCardKind.VIDEO.equals(kind)) {
            var t = nz(spec.getTitle());
            var vt = nz(spec.getVideoTitle());
            return !t.isBlank() ? t : vt;
        }
        if (WechatShareCardKind.AUDIO.equals(kind)) {
            var t = nz(spec.getTitle());
            var d = nz(spec.getDisplayName());
            return !t.isBlank() ? t : d;
        }
        if (WechatShareCardKind.IMAGE.equals(kind)) {
            var t = nz(spec.getTitle());
            var d = nz(spec.getDisplayName());
            return !t.isBlank() ? t : d;
        }
        return nz(spec.getTitle());
    }

    private static String resolveShareDesc(String kind, WechatShareCard.Spec spec) {
        if (WechatShareCardKind.VIDEO.equals(kind)) {
            var guide = nz(spec.getVideoGuideText());
            var desc = nz(spec.getDescription());
            var raw = !guide.isBlank() ? guide : desc;
            return WechatShareCardService.shareSnippet(raw, 120);
        }
        return WechatShareCardService.shareSnippet(nz(spec.getDescription()), 120);
    }

    private void appendShellForKind(
        StringBuilder sb,
        String kind,
        WechatShareCard.Spec spec,
        WeChatJsBridgeService.Signature sig,
        boolean showShareHint
    ) {
        switch (kind) {
            case WechatShareCardKind.IMAGE -> appendImageShell(sb, spec, sig, showShareHint);
            case WechatShareCardKind.AUDIO -> appendAudioShell(sb, spec, sig, showShareHint);
            case WechatShareCardKind.VIDEO -> appendVideoShell(sb, spec, sig, showShareHint);
            case WechatShareCardKind.FILE -> appendFileShell(sb, spec, sig, showShareHint);
            default -> appendLinkShell(sb, spec, sig, showShareHint);
        }
    }

    private static void appendSdkBlocks(StringBuilder sb, WeChatJsBridgeService.Signature sig) {
        if (sig.usable()) {
            sb.append("<div id=\"wx-jssdk-banner\" class=\"sdk-banner\" role=\"alert\"></div>\n");
        } else {
            sb.append("<div class=\"sig-static-hint\" role=\"status\">");
            sb.append("<p>未正确配置公众号信息，");
            sb.append("请到插件「插件配置」填写 AppId、AppSecret。</p>");
            sb.append("</div>\n");
        }
    }

    private void appendLinkShell(StringBuilder sb, WechatShareCard.Spec spec, WeChatJsBridgeService.Signature sig, boolean showShareHint) {
        var title = nz(spec.getTitle());
        var desc = nz(spec.getDescription());
        sb.append("<div class=\"wrap\"><main class=\"sheet\">");
        sb.append("<h1>").append(HtmlEscapes.text(title.isBlank() ? "微信分享" : title)).append("</h1>");
        if (!desc.isBlank()) {
            sb.append("<p class=\"sub\">").append(HtmlEscapes.text(desc)).append("</p>");
        }
        appendSdkBlocks(sb, sig);
        if (showShareHint && sig.usable()) {
            sb.append("<p class=\"note\">右上角分享至朋友或朋友圈后此文案将隐藏</p>");
        }
        sb.append("</main></div>\n");
    }

    private void appendImageShell(StringBuilder sb, WechatShareCard.Spec spec, WeChatJsBridgeService.Signature sig, boolean showShareHint) {
        var title = nz(spec.getTitle());
        var dn = nz(spec.getDisplayName());
        var img = nz(spec.getImg());
        var intro = nz(spec.getDescription());
        var headlineRaw = !title.isBlank() ? title : dn;
        var headline = headlineRaw.isBlank() ? "图片" : headlineRaw;

        sb.append("<div class=\"img-shell\"><div class=\"img-stack\">");
        sb.append("<div class=\"img-card\">");
        if (!img.isBlank()) {
            sb.append("<div class=\"img-frame\"><img class=\"img-photo\" src=\"").append(HtmlEscapes.text(img))
                .append("\" alt=\"\" loading=\"lazy\" decoding=\"async\"/></div>");
        }
        sb.append("<div class=\"img-body\">");
        sb.append("<h1 class=\"img-title\">").append(HtmlEscapes.text(headline)).append("</h1>");
        if (!intro.isBlank()) {
            sb.append("<p class=\"img-intro\">").append(HtmlEscapes.text(intro)).append("</p>");
        }
        appendContactModern(sb, spec, "img");
        appendSdkBlocks(sb, sig);
        if (showShareHint && sig.usable()) {
            sb.append("<p class=\"img-foot\">右上角分享至朋友或朋友圈后此文案将隐藏</p>");
        }
        sb.append("</div></div>");
        appendKindNotesSection(sb, spec, "in");
        sb.append("</div></div>\n");
    }

    private void appendAudioShell(StringBuilder sb, WechatShareCard.Spec spec, WeChatJsBridgeService.Signature sig, boolean showShareHint) {
        var sid = nz(spec.getSid());
        var title = nz(spec.getTitle());
        var displayName = nz(spec.getDisplayName());
        var cover = nz(spec.getImg());
        var audioUrl = nz(spec.getMediaUrl());
        var intro = nz(spec.getDescription());
        var headlineRaw = !title.isBlank() ? title : displayName;
        var headline = headlineRaw.isBlank() ? "未命名曲目" : headlineRaw;
        var artistLine = intro.isBlank() ? "纯音乐，请欣赏" : intro;

        sb.append("<div class=\"au-shell\">");
        if (!cover.isBlank()) {
            sb.append("<div class=\"au-bg\" aria-hidden=\"true\"><img class=\"au-bg__img\" src=\"")
                .append(HtmlEscapes.text(cover))
                .append("\" alt=\"\"/></div>");
        } else {
            sb.append("<div class=\"au-bg au-bg--fallback\" aria-hidden=\"true\"></div>");
        }
        sb.append("<div class=\"au-inner\">");

        if (!audioUrl.isBlank()) {
            sb.append("<div class=\"au-root\" data-ap=\"").append(HtmlEscapes.text(sid)).append("\">");
            sb.append("<div class=\"au-vinyl-stage\">");
            sb.append("<div class=\"au-arm\" aria-hidden=\"true\"><span class=\"au-arm-head\"></span></div>");
            sb.append("<div class=\"au-disc-outer\"><div class=\"au-groove\" aria-hidden=\"true\"></div>");
            sb.append("<div class=\"au-disc\">");
            sb.append("<div class=\"au-art-wrap\">");
            if (!cover.isBlank()) {
                sb.append("<img class=\"au-art\" src=\"").append(HtmlEscapes.text(cover)).append("\" alt=\"\" loading=\"lazy\" decoding=\"async\"/>");
            } else {
                sb.append("<div class=\"au-art au-art--ph\" aria-hidden=\"true\"></div>");
            }
            sb.append("</div>");
            sb.append("<button type=\"button\" class=\"au-play\" aria-label=\"播放/暂停\">");
            sb.append("<span class=\"au-play__ring\" aria-hidden=\"true\"></span>");
            sb.append("<span class=\"au-play__ic\" aria-hidden=\"true\"></span>");
            sb.append("</button>");
            sb.append("</div></div></div>");

            sb.append("<audio id=\"ws-audio-").append(HtmlEscapes.text(sid)).append("\" preload=\"metadata\" style=\"display:none\">");
            sb.append("<source src=\"").append(HtmlEscapes.text(audioUrl)).append("\" type=\"audio/mpeg\"/></audio>");

            sb.append("<div class=\"au-scrub\">");
            sb.append("<div class=\"au-time\"><span class=\"au-cur\">0:00</span><span class=\"au-sep\">/</span><span class=\"au-total\">0:00</span></div>");
            sb.append("<div class=\"au-track\"><div class=\"au-fill\"></div></div>");
            sb.append("</div></div>");
        } else {
            sb.append("<div class=\"au-vinyl-stage au-vinyl-stage--static\">");
            sb.append("<div class=\"au-arm\" aria-hidden=\"true\"><span class=\"au-arm-head\"></span></div>");
            sb.append("<div class=\"au-disc-outer\"><div class=\"au-groove\" aria-hidden=\"true\"></div><div class=\"au-disc\">");
            sb.append("<div class=\"au-art-wrap\">");
            if (!cover.isBlank()) {
                sb.append("<img class=\"au-art\" src=\"").append(HtmlEscapes.text(cover)).append("\" alt=\"\" loading=\"lazy\" decoding=\"async\"/>");
            } else {
                sb.append("<div class=\"au-art au-art--ph\" aria-hidden=\"true\"></div>");
            }
            sb.append("</div></div></div></div>");
        }

        sb.append("<div class=\"au-meta\">");
        sb.append("<p class=\"au-title\">").append(HtmlEscapes.text(headline)).append("</p>");
        sb.append("<p class=\"au-artist\">").append(HtmlEscapes.text(artistLine)).append("</p>");
        sb.append("</div>");

        appendContactModern(sb, spec, "au");
        appendSdkBlocks(sb, sig);
        if (showShareHint && sig.usable()) {
            sb.append("<p class=\"au-foot\">右上角分享至朋友或朋友圈后此文案将隐藏</p>");
        }
        sb.append("</div></div>\n");
    }

    private void appendVideoShell(StringBuilder sb, WechatShareCard.Spec spec, WeChatJsBridgeService.Signature sig, boolean showShareHint) {
        var pageTitle = nz(spec.getTitle());
        var cover = nz(spec.getImg());
        var videoUrl = nz(spec.getMediaUrl());
        var vt = nz(spec.getVideoTitle());
        var guide = nz(spec.getVideoGuideText());
        var desc = nz(spec.getDescription());
        var extra = nz(spec.getVideoExtraLink());
        var extraLabel = nz(spec.getVideoExtraLinkLabel());
        var headlineRaw = !pageTitle.isBlank() ? pageTitle : vt;
        var headline = headlineRaw.isBlank() ? "视频" : headlineRaw;
        var caption = !guide.isBlank() ? guide : desc;
        var showExpand = caption.length() > 96;
        var chipText = extraLabel.isBlank() ? "相关链接" : extraLabel;

        sb.append("<div class=\"vv-stage\">");
        sb.append("<div class=\"vv-viewport\">");
        sb.append("<div class=\"vv-video-wrap\">");
        if (!videoUrl.isBlank()) {
            var poster = cover.isBlank() ? "" : HtmlEscapes.text(cover);
            sb.append("<video id=\"vv-video\" class=\"vv-video\" playsinline webkit-playsinline preload=\"metadata\" ");
            if (!poster.isBlank()) {
                sb.append("poster=\"").append(poster).append("\" ");
            }
            sb.append("src=\"").append(HtmlEscapes.text(videoUrl)).append("\"></video>");
            sb.append("<button type=\"button\" id=\"vv-bigplay\" class=\"vv-bigplay\" aria-label=\"播放/暂停\">");
            sb.append("<span class=\"vv-bigplay__tri\" aria-hidden=\"true\"></span></button>");
            sb.append("<div class=\"vv-grad\" aria-hidden=\"true\"></div>");
            sb.append("<div class=\"vv-ui\">");
            sb.append("<div class=\"vv-dock\">");
            if (!extra.isBlank()) {
                sb.append("<a class=\"vv-chip\" href=\"").append(HtmlEscapes.text(extra)).append("\" rel=\"noopener\">");
                sb.append("<span class=\"vv-chip__t\">")
                    .append(HtmlEscapes.text(chipText))
                    .append("</span>");
                sb.append("<span class=\"vv-chip__go\" aria-hidden=\"true\"></span></a>");
            }
            sb.append("<p class=\"vv-title\">").append(HtmlEscapes.text(headline)).append("</p>");
            if (!caption.isBlank()) {
                sb.append("<div class=\"vv-descbox");
                if (showExpand) {
                    sb.append(" vv-descbox--clamp");
                }
                sb.append("\" id=\"vv-descbox\">");
                sb.append("<p class=\"vv-desc\" id=\"vv-desc\">").append(HtmlEscapes.text(caption)).append("</p>");
                if (showExpand) {
                    sb.append("<button type=\"button\" class=\"vv-more\" id=\"vv-more\" aria-expanded=\"false\">展开</button>");
                }
                sb.append("</div>");
            }
            sb.append("<div class=\"vv-prog\" id=\"vv-prog\" role=\"slider\" aria-label=\"进度\" tabindex=\"0\">");
            sb.append("<div class=\"vv-prog-fill\" id=\"vv-prog-fill\"></div></div>");
            sb.append("</div></div></div>");
        } else {
            sb.append("<div class=\"vv-empty\">暂无视频地址</div>");
            sb.append("</div>");
        }
        sb.append("<div class=\"vv-foot\">");
        appendSdkBlocks(sb, sig);
        if (showShareHint && sig.usable()) {
            sb.append("<p class=\"vv-hint\">右上角分享至朋友或朋友圈后此文案将隐藏</p>");
        }
        sb.append("</div></div></div>\n");
    }

    private void appendFileShell(StringBuilder sb, WechatShareCard.Spec spec, WeChatJsBridgeService.Signature sig, boolean showShareHint) {
        var title = nz(spec.getTitle());
        var cover = nz(spec.getImg());
        var fileUrl = nz(spec.getMediaUrl());
        var fileName = nz(spec.getDisplayName());
        var intro = nz(spec.getDescription());
        var contact = nz(spec.getContactInfo());

        var primary = !fileName.isBlank() ? fileName : title;
        if (primary.isBlank()) {
            primary = "文件下载";
        }
        var secondary = !fileName.isBlank() && !title.isBlank() && !fileName.equals(title) ? title : "";

        sb.append("<div class=\"fp-container\">");
        sb.append("<div class=\"fp-card\">");
        sb.append("<div class=\"fp-cover-wrap\">");
        if (!cover.isBlank()) {
            sb.append("<img class=\"fp-cover\" src=\"").append(HtmlEscapes.text(cover)).append("\" alt=\"\" loading=\"lazy\" decoding=\"async\"/>");
        } else {
            sb.append("<div class=\"fp-cover fp-cover--ph\" aria-hidden=\"true\"></div>");
        }
        sb.append("</div>");
        sb.append("<h1 class=\"fp-title\">").append(HtmlEscapes.text(primary)).append("</h1>");
        if (!secondary.isBlank()) {
            sb.append("<p class=\"fp-subline\">").append(HtmlEscapes.text(secondary)).append("</p>");
        }
        if (!intro.isBlank()) {
            sb.append("<p class=\"fp-desc\">").append(HtmlEscapes.text(intro)).append("</p>");
        }
        appendSdkBlocks(sb, sig);
        if (!fileUrl.isBlank()) {
            sb.append("<a class=\"fp-dl\" href=\"").append(HtmlEscapes.text(fileUrl)).append("\" rel=\"noopener\">下载</a>");
            sb.append("<p class=\"fp-tip\">若微信内拦截下载，可使用右上角菜单「在浏览器打开」。</p>");
        } else {
            sb.append("<p class=\"fp-empty\">暂无可用的下载地址，请稍后再试。</p>");
        }
        sb.append("</div>");

        appendKindNotesSection(sb, spec, "fp");

        if (!contact.isBlank()) {
            sb.append("<footer class=\"fp-foot\">").append(HtmlEscapes.text(contact)).append("</footer>");
        }

        if (showShareHint && sig.usable()) {
            sb.append("<p class=\"fp-hint\">右上角分享至朋友或朋友圈后此文案将隐藏");
        }
        sb.append("</div>\n");
    }


    private void appendKindNotesSection(StringBuilder sb, WechatShareCard.Spec spec, String ns) {
        var notes = spec.getFileNotes();
        if (notes == null || notes.isEmpty()) {
            var legacyUrl = nz(spec.getOptionalLinkUrl());
            if (!legacyUrl.isBlank()) {
                var legacyLabel = nz(spec.getOptionalLinkLabel());
                var linkTitle = legacyLabel.isBlank() ? "查看详情" : legacyLabel;
                sb.append("<div class=\"").append(ns).append("-card\"><div class=\"").append(ns).append("-section\">相关说明</div><div class=\"")
                    .append(ns).append("-links\">");
                sb.append("<a class=\"").append(ns).append("-link\" href=\"").append(HtmlEscapes.text(legacyUrl)).append("\" rel=\"noopener\">");
                sb.append("<span class=\"").append(ns).append("-link-main\"><span class=\"").append(ns).append("-link-t\">")
                    .append(HtmlEscapes.text(linkTitle)).append("</span>");
                sb.append("<span class=\"").append(ns).append("-link-d\">在浏览器中打开</span></span>");
                sb.append("<span class=\"").append(ns).append("-arrow\" aria-hidden=\"true\"></span></a>");
                sb.append("</div></div>");
            }
            return;
        }
        var inner = new StringBuilder();
        var any = false;
        for (var n : notes) {
            if (n == null) {
                continue;
            }
            var t = nz(n.getTitle());
            var d = nz(n.getDetail());
            var u = nz(n.getUrl());
            var jump = Boolean.TRUE.equals(n.getJumpLink());
            if (t.isBlank() && d.isBlank() && (!jump || u.isBlank())) {
                continue;
            }
            any = true;
            if (jump && !u.isBlank()) {
                inner.append("<a class=\"").append(ns).append("-link\" href=\"").append(HtmlEscapes.text(u)).append("\" rel=\"noopener\">");
                inner.append("<span class=\"").append(ns).append("-link-main\">");
                inner.append("<span class=\"").append(ns).append("-link-t\">").append(HtmlEscapes.text(t.isBlank() ? "链接" : t)).append("</span>");
                if (!d.isBlank()) {
                    inner.append("<span class=\"").append(ns).append("-link-d\">").append(HtmlEscapes.text(d)).append("</span>");
                } else {
                    inner.append("<span class=\"").append(ns).append("-link-d\">在浏览器中打开</span>");
                }
                inner.append("</span><span class=\"").append(ns).append("-arrow\" aria-hidden=\"true\"></span></a>");
            } else {
                inner.append("<div class=\"").append(ns).append("-note\">");
                if (!t.isBlank()) {
                    inner.append("<p class=\"").append(ns).append("-note-t\">").append(HtmlEscapes.text(t)).append("</p>");
                }
                if (!d.isBlank()) {
                    inner.append("<p class=\"").append(ns).append("-note-d\">").append(HtmlEscapes.text(d)).append("</p>");
                }
                inner.append("</div>");
            }
        }
        if (!any) {
            return;
        }
        sb.append("<div class=\"").append(ns).append("-card\"><div class=\"").append(ns).append("-section\">相关说明</div><div class=\"")
            .append(ns).append("-links\">");
        sb.append(inner);
        sb.append("</div></div>");
    }

    private static void appendOptionalLinkModern(StringBuilder sb, WechatShareCard.Spec spec, String pfx) {
        var label = nz(spec.getOptionalLinkLabel());
        var url = nz(spec.getOptionalLinkUrl());
        if (label.isBlank() && url.isBlank()) {
            return;
        }
        sb.append("<div class=\"").append(pfx).append("-opt\">");
        sb.append("<span>").append(HtmlEscapes.text(label.isBlank() ? "相关链接" : label)).append("</span>");
        if (!url.isBlank()) {
            sb.append("<a class=\"").append(pfx).append("-opt-a\" href=\"").append(HtmlEscapes.text(url)).append("\" rel=\"noopener\">查看</a>");
        }
        sb.append("</div>");
    }

    private static void appendContactModern(StringBuilder sb, WechatShareCard.Spec spec, String pfx) {
        var c = nz(spec.getContactInfo());
        if (c.isBlank()) {
            return;
        }
        sb.append("<p class=\"").append(pfx).append("-contact\">").append(HtmlEscapes.text(c)).append("</p>");
    }

    private static void appendStylesForKind(StringBuilder sb, String kind) {
        ShareLandingCss.appendHarmonySans(sb);
        ShareLandingCss.appendKind(sb, kind);
        ShareLandingCss.appendSharedBanner(sb);
    }

    private static void appendAudioUiScript(StringBuilder sb, WechatShareCard.Spec spec) {
        var sid = nz(spec.getSid());
        sb.append("<script>\n");
        sb.append("(function(){\n");
        sb.append("  var audio=document.getElementById(\"ws-audio-").append(HtmlEscapes.text(sid)).append("\");\n");
        sb.append("  if(!audio)return;var root=document.querySelector('[data-ap=\"").append(HtmlEscapes.text(sid)).append("\"]');if(!root)return;\n");
        sb.append("  var btn=root.querySelector('.au-play');var fill=root.querySelector('.au-fill');var track=root.querySelector('.au-track');\n");
        sb.append("  var cur=root.querySelector('.au-cur');var total=root.querySelector('.au-total');\n");
        sb.append("  function fmt(t){if(!isFinite(t)||t<0)t=0;var m=Math.floor(t/60),s=Math.floor(t%60);return m+\":\"+(s<10?\"0\":\"\")+s;}\n");
        sb.append("  audio.addEventListener('loadedmetadata',function(){total.textContent=fmt(audio.duration||0);});\n");
        sb.append("  audio.addEventListener('timeupdate',function(){var d=audio.duration||1;var p=audio.currentTime/d;fill.style.width=(p*100).toFixed(2)+'%';");
        sb.append("cur.textContent=fmt(audio.currentTime);});\n");
        sb.append("  function setPlaying(on){btn.classList.toggle('is-playing',on);root.classList.toggle('is-playing',on);}\n");
        sb.append("  audio.addEventListener('play',function(){setPlaying(true);});\n");
        sb.append("  audio.addEventListener('pause',function(){setPlaying(false);});\n");
        sb.append("  audio.addEventListener('ended',function(){setPlaying(false);});\n");
        sb.append("  btn.addEventListener('click',function(){if(audio.paused){audio.play();}else{audio.pause();}});\n");
        sb.append("  track.addEventListener('click',function(e){var r=track.getBoundingClientRect();var x=Math.min(Math.max(e.clientX-r.left,0),r.width);");
        sb.append("var d=audio.duration||0;if(d){audio.currentTime=(x/r.width)*d;}});\n");
        sb.append("})();\n");
        sb.append("</script>\n");
    }

    private static void appendVideoUiScript(StringBuilder sb) {
        sb.append("<script>\n");
        sb.append("(function(){\n");
        sb.append("  var v=document.getElementById('vv-video');var bp=document.getElementById('vv-bigplay');\n");
        sb.append("  var fill=document.getElementById('vv-prog-fill');var prog=document.getElementById('vv-prog');\n");
        sb.append("  var more=document.getElementById('vv-more');var box=document.getElementById('vv-descbox');\n");
        sb.append("  function syncPlayBtn(){if(!bp)return;if(v.paused){bp.classList.remove('is-hide');}else{bp.classList.add('is-hide');}}\n");
        sb.append("  function syncProg(){if(!fill||!v)return;var d=v.duration||0;if(!d)return;var p=v.currentTime/d;fill.style.width=(p*100).toFixed(2)+'%';}\n");
        sb.append("  if(v&&bp){\n");
        sb.append("    v.addEventListener('play',syncPlayBtn);v.addEventListener('pause',syncPlayBtn);syncPlayBtn();\n");
        sb.append("    v.addEventListener('timeupdate',syncProg);v.addEventListener('loadedmetadata',syncProg);\n");
        sb.append("    bp.addEventListener('click',function(e){e.stopPropagation();if(v.paused){v.play();}else{v.pause();}});\n");
        sb.append("    v.addEventListener('click',function(){if(v.paused){v.play();}else{v.pause();}});\n");
        sb.append("  }\n");
        sb.append("  if(prog&&v){\n");
        sb.append("    prog.addEventListener('click',function(e){var r=prog.getBoundingClientRect();");
        sb.append("var x=Math.min(Math.max(e.clientX-r.left,0),r.width);var d=v.duration||0;if(d){v.currentTime=(x/r.width)*d;}});\n");
        sb.append("  }\n");
        sb.append("  if(more&&box){\n");
        sb.append("    more.addEventListener('click',function(){var open=!box.classList.contains('is-open');");
        sb.append("box.classList.toggle('is-open',open);more.setAttribute('aria-expanded',open?'true':'false');");
        sb.append("more.textContent=open?'收起':'展开';});\n");
        sb.append("  }\n");
        sb.append("})();\n");
        sb.append("</script>\n");
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private void appendWxScripts(
        StringBuilder sb,
        WeChatJsBridgeService.Signature sig,
        String title,
        String desc,
        String img,
        String wechatShareLink
    ) throws Exception {
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

        sb.append("<script>\n");
        sb.append("(function () {\n");
        sb.append("  var u = ").append(objectMapper.writeValueAsString(WECHAT_JSSDK_URL)).append(";\n");
        sb.append(
            "  var loadFail = '无法加载微信 JSSDK 脚本，自定义分享不可用。"
                + "请检查网络或稍后重试；若域名/网络拦截了对 res.wx.qq.com 的访问也会导致此问题。';\n");
        sb.append(
            "  var cfgFail = '微信 JSSDK 校验失败：请核对插件中的公众号 AppId / AppSecret、"
                + "JS 接口安全域名是否与当前页面域名一致，并确认「外部访问地址」与签名所用链接一致。';\n");
        sb.append("  function showBanner(text) {\n");
        sb.append("    var el = document.getElementById('wx-jssdk-banner');\n");
        sb.append("    if (!el) return;\n");
        sb.append("    el.textContent = text;\n");
        sb.append("    el.style.display = 'block';\n");
        sb.append("  }\n");
        sb.append("  var s = document.createElement('script');\n");
        sb.append("  s.src = u;\n");
        sb.append("  s.async = true;\n");
        sb.append("  s.onerror = function () { showBanner(loadFail); };\n");
        sb.append("  s.onload = function () {\n");
        sb.append("    if (typeof wx === 'undefined') {\n");
        sb.append(
            "      showBanner('微信 JSSDK 已请求但未暴露 wx 对象，无法继续配置分享。请更换微信内置浏览器重试。');\n");
        sb.append("      return;\n");
        sb.append("    }\n");
        sb.append("    wx.config(").append(cfgJson).append(");\n");
        sb.append("    wx.ready(function () {\n");
        sb.append("      wx.updateTimelineShareData(").append(timelineJson).append(");\n");
        sb.append("      wx.updateAppMessageShareData(").append(appMsgJson).append(");\n");
        sb.append("    });\n");
        sb.append("    wx.error(function (res) {\n");
        sb.append("      try { console.log(res); } catch (e) {}\n");
        sb.append("      showBanner(cfgFail);\n");
        sb.append("    });\n");
        sb.append("  };\n");
        sb.append("  document.head.appendChild(s);\n");
        sb.append("})();\n");
        sb.append("</script>\n");
    }
}
