package com.avrinbai.wechatshare.web;

import com.avrinbai.wechatshare.WechatShareCardKind;
import com.avrinbai.wechatshare.WechatShareCardStates;
import com.avrinbai.wechatshare.extension.WechatShareCard;
import com.avrinbai.wechatshare.service.WechatShareCardService;
import com.avrinbai.wechatshare.service.WechatShareSettingsService;
import com.avrinbai.wechatshare.service.WechatShareVisitRecorder;
import com.avrinbai.wechatshare.support.VisitHitType;
import com.avrinbai.wechatshare.support.HtmlEscapes;
import com.avrinbai.wechatshare.support.HttpUrls;
import com.avrinbai.wechatshare.support.PublicUrls;
import com.avrinbai.wechatshare.support.ShareRoutePaths;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class WechatShareSiteHandler {

    private final WechatShareCardService cardService;
    private final WechatShareSettingsService settingsService;
    private final WechatSharePageRenderer sharePageRenderer;
    private final WechatShareVisitRecorder visitRecorder;

    public WechatShareSiteHandler(
        WechatShareCardService cardService,
        WechatShareSettingsService settingsService,
        WechatSharePageRenderer sharePageRenderer,
        WechatShareVisitRecorder visitRecorder
    ) {
        this.cardService = cardService;
        this.settingsService = settingsService;
        this.sharePageRenderer = sharePageRenderer;
        this.visitRecorder = visitRecorder;
    }

    /** 推广入口：扫码 / 复制链接；媒体类带引导文案。 */
    public Mono<ServerResponse> share(ServerRequest request, String publicBasePath) {
        var sid = resolveSid(request, publicBasePath, ShareRoutePaths.ACTION_SHARE);
        if (sid.isBlank()) {
            return notFound("链接无效：缺少卡片标识。请从控制台重新复制分享链接或二维码。");
        }
        return settingsService.load()
            .publishOn(Schedulers.boundedElastic())
            .flatMap(settings -> Mono.fromCallable(() -> cardService.findBySid(sid).orElse(null))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(card -> {
                    if (card == null) {
                        return notFound("链接不存在或已删除");
                    }
                    if (!WechatShareCardStates.isEnabled(card)) {
                        return notFound("链接已停用或不存在");
                    }
                    var site = settingsService.resolveExternalSiteUrl(settings);
                    var basePath = normalizedBasePath(settings);
                    var kind = WechatShareCardKind.normalize(card.getSpec().getCardKind());

                    // 兼容旧分享链接：/share/{sid}?hint=0 → /view/{sid}
                    var hintParam = request.queryParam("hint").orElse("");
                    if (!WechatShareCardKind.LINK.equals(kind) && "0".equals(hintParam)) {
                        return redirectTo(site, ShareRoutePaths.viewPathWithSid(basePath, sid));
                    }

                    return renderLanding(
                        request,
                        card,
                        settings,
                        site,
                        basePath,
                        true,
                        VisitHitType.SHARE
                    );
                }));
    }

    /** 媒体类卡片二次分享落地页：无引导文案。链接类访问时重定向至 /share。 */
    public Mono<ServerResponse> view(ServerRequest request, String publicBasePath) {
        var sid = resolveSid(request, publicBasePath, ShareRoutePaths.ACTION_VIEW);
        if (sid.isBlank()) {
            return notFound("链接无效：缺少卡片标识。请从控制台重新复制分享链接或二维码。");
        }
        return settingsService.load()
            .publishOn(Schedulers.boundedElastic())
            .flatMap(settings -> Mono.fromCallable(() -> cardService.findBySid(sid).orElse(null))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(card -> {
                    if (card == null) {
                        return notFound("链接不存在或已删除");
                    }
                    if (!WechatShareCardStates.isEnabled(card)) {
                        return notFound("链接已停用或不存在");
                    }
                    var site = settingsService.resolveExternalSiteUrl(settings);
                    var basePath = normalizedBasePath(settings);
                    var kind = WechatShareCardKind.normalize(card.getSpec().getCardKind());

                    if (WechatShareCardKind.LINK.equals(kind)) {
                        return redirectTo(site, ShareRoutePaths.sharePathWithSid(basePath, sid));
                    }

                    return renderLanding(
                        request,
                        card,
                        settings,
                        site,
                        basePath,
                        false,
                        VisitHitType.VIEW
                    );
                }));
    }

    public Mono<ServerResponse> go(ServerRequest request, String publicBasePath) {
        var sid = resolveSid(request, publicBasePath, ShareRoutePaths.ACTION_GO);
        if (sid.isBlank()) {
            return notFound("链接无效：缺少卡片标识。请从控制台重新复制分享链接或二维码。");
        }
        return Mono.fromCallable(() -> cardService.findBySid(sid).orElse(null))
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(card -> {
                if (card == null || card.getSpec() == null || card.getSpec().getRedirectUrl() == null) {
                    return notFound("链接不存在或已删除");
                }
                if (!WechatShareCardStates.isEnabled(card)) {
                    return notFound("链接已停用或不存在");
                }
                var raw = card.getSpec().getRedirectUrl().trim();
                var target = HttpUrls.normalize(raw);
                try {
                    var uri = URI.create(target);
                    var scheme = uri.getScheme();
                    if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                        return badRequest("跳转地址无效");
                    }
                    visitRecorder.recordAsync(request, sid, VisitHitType.GO);
                    return ServerResponse.status(HttpStatus.FOUND).location(uri).build();
                } catch (Exception e) {
                    return badRequest("跳转地址无效");
                }
            });
    }

    private Mono<ServerResponse> renderLanding(
        ServerRequest request,
        WechatShareCard card,
        com.avrinbai.wechatshare.extension.WechatShareSettings settings,
        String site,
        String basePath,
        boolean showShareHint,
        String hitType
    ) {
        try {
            var sid = card.getSpec().getSid();
            var signUrl = buildWxJsSdkSignUrl(request, site);
            var kind = WechatShareCardKind.normalize(card.getSpec().getCardKind());

            String wechatShareLink;
            String qqShareLink;
            if (WechatShareCardKind.LINK.equals(kind)) {
                var goPath = ShareRoutePaths.goPathWithSid(basePath, sid);
                wechatShareLink = PublicUrls.absoluteHttp(site, goPath);
                qqShareLink = wechatShareLink;
            } else {
                var viewPath = ShareRoutePaths.viewPathWithSid(basePath, sid);
                wechatShareLink = PublicUrls.absoluteHttp(site, viewPath);
                qqShareLink = wechatShareLink;
            }

            var html = sharePageRenderer.render(
                card,
                settings,
                signUrl,
                wechatShareLink,
                qqShareLink,
                showShareHint,
                site
            );
            visitRecorder.recordAsync(request, sid, hitType);
            return ServerResponse.ok().contentType(MediaType.TEXT_HTML).bodyValue(html);
        } catch (Exception e) {
            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.TEXT_HTML)
                .bodyValue(notFoundHtml("页面生成失败"));
        }
    }

    private static String normalizedBasePath(com.avrinbai.wechatshare.extension.WechatShareSettings settings) {
        return WechatShareSettingsService.normalizePath(
            settings.getSpec() == null ? null : settings.getSpec().getPublicBasePath(),
            WechatShareSettingsService.DEFAULT_PUBLIC_BASE_PATH
        );
    }

    private static Mono<ServerResponse> redirectTo(String site, String path) {
        var target = PublicUrls.absoluteHttp(site, path);
        return ServerResponse.status(HttpStatus.FOUND).location(URI.create(target)).build();
    }

    private static Mono<ServerResponse> notFound(String msg) {
        return ServerResponse.status(HttpStatus.NOT_FOUND)
            .contentType(MediaType.TEXT_HTML)
            .bodyValue(notFoundHtml(msg));
    }

    private static Mono<ServerResponse> badRequest(String msg) {
        return ServerResponse.status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.TEXT_HTML)
            .bodyValue(notFoundHtml(msg));
    }

    private static String resolveSid(ServerRequest request, String publicBasePath, String action) {
        var querySid = request.queryParam("sid").orElse("");
        var path = request.path();
        return ShareRoutePaths.resolveSid(path, publicBasePath, action, querySid);
    }

    static String buildWxJsSdkSignUrl(ServerRequest request, String externalSiteRoot) {
        var path = request.path();
        var rawQuery = request.uri().getRawQuery();
        if (externalSiteRoot == null || externalSiteRoot.isBlank()) {
            return request.uri().toString().split("#")[0];
        }
        var base = externalSiteRoot.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        var p = (path == null || path.isBlank()) ? "" : (path.startsWith("/") ? path : "/" + path);
        if (rawQuery == null || rawQuery.isBlank()) {
            return base + p;
        }
        return base + p + "?" + rawQuery;
    }

    private static String notFoundHtml(String msg) {
        return "<!DOCTYPE html><html lang=\"zh-CN\"><head><meta charset=\"utf-8\">"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
            + "<title>提示</title></head><body>"
            + "<p style=\"text-align:center;margin-top:200px;font-size:18px;color:#666;\">"
            + HtmlEscapes.text(msg)
            + "</p></body></html>";
    }
}
