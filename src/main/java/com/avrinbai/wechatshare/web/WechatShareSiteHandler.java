package com.avrinbai.wechatshare.web;

import com.avrinbai.wechatshare.WechatShareCardKind;
import com.avrinbai.wechatshare.WechatShareCardStates;
import com.avrinbai.wechatshare.service.WechatShareCardService;
import com.avrinbai.wechatshare.service.WechatShareSettingsService;
import com.avrinbai.wechatshare.service.WechatShareVisitRecorder;
import com.avrinbai.wechatshare.support.VisitHitType;
import com.avrinbai.wechatshare.support.HtmlEscapes;
import com.avrinbai.wechatshare.support.HttpUrls;
import com.avrinbai.wechatshare.support.PublicUrls;
import java.net.URI;
import java.nio.charset.StandardCharsets;
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

    public Mono<ServerResponse> share(ServerRequest request) {
        var sid = request.queryParam("sid").orElse("").trim();
        return settingsService.load()
            .publishOn(Schedulers.boundedElastic())
            .flatMap(settings -> Mono.fromCallable(() -> cardService.findBySid(sid).orElse(null))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(card -> {
                    if (card == null) {
                        return ServerResponse.status(HttpStatus.NOT_FOUND)
                            .contentType(MediaType.TEXT_HTML)
                            .bodyValue(notFoundHtml("链接不存在或已删除"));
                    }
                    if (!WechatShareCardStates.isEnabled(card)) {
                        return ServerResponse.status(HttpStatus.NOT_FOUND)
                            .contentType(MediaType.TEXT_HTML)
                            .bodyValue(notFoundHtml("链接已停用或不存在"));
                    }
                    try {
                        var site = settingsService.resolveExternalSiteUrl(settings);
                        var signUrl = buildWxJsSdkSignUrl(request, site);
                        var basePath = WechatShareSettingsService.normalizePath(
                            settings.getSpec() == null ? null : settings.getSpec().getPublicBasePath(),
                            WechatShareSettingsService.DEFAULT_PUBLIC_BASE_PATH
                        );
                        var kind = WechatShareCardKind.normalize(card.getSpec().getCardKind());

                        String wechatShareLink;
                        if (WechatShareCardKind.LINK.equals(kind)) {
                            wechatShareLink = PublicUrls.absoluteHttp(
                                site,
                                basePath + "/go?sid=" + java.net.URLEncoder.encode(sid, StandardCharsets.UTF_8)
                            );
                        } else {
                            wechatShareLink = PublicUrls.absoluteHttp(
                                site,
                                basePath + "/share?sid=" + java.net.URLEncoder.encode(sid, StandardCharsets.UTF_8)
                                    + "&hint=0"
                            );
                        }

                        var hintParam = request.queryParam("hint").orElse("");
                        var showShareHint = !"0".equals(hintParam);

                        var html = sharePageRenderer.render(card, settings, signUrl, wechatShareLink, showShareHint, site);
                        visitRecorder.recordAsync(request, sid, VisitHitType.SHARE);
                        return ServerResponse.ok().contentType(MediaType.TEXT_HTML).bodyValue(html);
                    } catch (Exception e) {
                        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .contentType(MediaType.TEXT_HTML)
                            .bodyValue(notFoundHtml("页面生成失败"));
                    }
                }));
    }

    public Mono<ServerResponse> go(ServerRequest request) {
        var sid = request.queryParam("sid").orElse("").trim();
        return Mono.fromCallable(() -> cardService.findBySid(sid).orElse(null))
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(card -> {
                if (card == null || card.getSpec() == null || card.getSpec().getRedirectUrl() == null) {
                    return ServerResponse.status(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.TEXT_HTML)
                        .bodyValue(notFoundHtml("链接不存在或已删除"));
                }
                if (!WechatShareCardStates.isEnabled(card)) {
                    return ServerResponse.status(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.TEXT_HTML)
                        .bodyValue(notFoundHtml("链接已停用或不存在"));
                }
                var raw = card.getSpec().getRedirectUrl().trim();
                var target = HttpUrls.normalize(raw);
                try {
                    var uri = URI.create(target);
                    var scheme = uri.getScheme();
                    if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                        return ServerResponse.status(HttpStatus.BAD_REQUEST)
                            .contentType(MediaType.TEXT_HTML)
                            .bodyValue(notFoundHtml("跳转地址无效"));
                    }
                    visitRecorder.recordAsync(request, sid, VisitHitType.GO);
                    return ServerResponse.status(HttpStatus.FOUND).location(uri).build();
                } catch (Exception e) {
                    return ServerResponse.status(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.TEXT_HTML)
                        .bodyValue(notFoundHtml("跳转地址无效"));
                }
            });
    }

    /**
     * 与微信客户端用于签名的页面 URL 对齐：优先使用 Halo「外部访问地址」作为 scheme+host，
     * path 与 query 与当前请求一致（保留 rawQuery 编码顺序）。
     */
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
