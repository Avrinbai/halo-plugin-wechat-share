package com.avrinbai.wechatshare.service;

import com.avrinbai.wechatshare.ExtensionSchemeRegistry;
import com.avrinbai.wechatshare.WechatShareCardKind;
import com.avrinbai.wechatshare.extension.WechatShareCard;
import com.avrinbai.wechatshare.extension.WechatShareVisit;
import com.avrinbai.wechatshare.support.ClientIpResolver;
import com.avrinbai.wechatshare.support.VisitEnvKind;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;

@Service
public class WechatShareVisitRecorder {

    private static final Logger log = LoggerFactory.getLogger(WechatShareVisitRecorder.class);

    private static final int MAX_UA = 512;

    private final ReactiveExtensionClient client;
    private final ExtensionSchemeRegistry extensionSchemeRegistry;
    private final WechatShareCardService cardService;
    private final WechatShareStatsService statsService;

    public WechatShareVisitRecorder(
        ReactiveExtensionClient client,
        ExtensionSchemeRegistry extensionSchemeRegistry,
        WechatShareCardService cardService,
        WechatShareStatsService statsService
    ) {
        this.client = client;
        this.extensionSchemeRegistry = extensionSchemeRegistry;
        this.cardService = cardService;
        this.statsService = statsService;
    }

    public void recordAsync(ServerRequest request, String sid, String hitType) {
        Mono.fromRunnable(() -> recordBlocking(request, sid, hitType))
            .subscribeOn(Schedulers.boundedElastic())
            .doOnError(ex -> log.warn("visit record failed sid={} hit={} msg={}", sid, hitType, ex.toString()))
            .onErrorResume(ex -> Mono.empty())
            .subscribe();
    }

    private void recordBlocking(ServerRequest request, String sid, String hitType) {
        if (sid == null || sid.isBlank()) {
            return;
        }
        extensionSchemeRegistry.ensureRegistered();
        var uaRaw = request.headers().firstHeader("User-Agent");
        var ua = ClientIpResolver.truncate(uaRaw == null ? "" : uaRaw, MAX_UA);
        var env = VisitEnvKind.classify(ua);
        var ip = ClientIpResolver.resolve(request);
        var now = Instant.now().toEpochMilli();

        var cardOpt = cardService.findBySid(sid);
        String cardKind = WechatShareCardKind.LINK;
        if (cardOpt.isPresent() && cardOpt.get().getSpec() != null) {
            cardKind = WechatShareCardKind.normalize(cardOpt.get().getSpec().getCardKind());
        }

        var visit = new WechatShareVisit();
        var md = new Metadata();
        md.setName(UUID.randomUUID().toString());
        visit.setMetadata(md);
        var vs = new WechatShareVisit.Spec();
        vs.setSid(sid.trim());
        vs.setHitType(hitType);
        vs.setClientIp(ip);
        vs.setUserAgent(ua);
        vs.setEnvKind(env);
        vs.setVisitedAt(now);
        vs.setCardKind(cardKind);
        visit.setSpec(vs);
        client.create(visit).block();

        statsService.increment(env, now);

        if (cardOpt.isEmpty()) {
            return;
        }
        WechatShareCard card = cardOpt.get();
        if (card.getSpec() == null) {
            return;
        }
        long vc = card.getSpec().getVisitCount() == null ? 0L : card.getSpec().getVisitCount();
        card.getSpec().setVisitCount(vc + 1);
        client.update(card).block();
    }
}
