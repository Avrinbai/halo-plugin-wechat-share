package com.avrinbai.wechatshare.service;

import com.avrinbai.wechatshare.ExtensionSchemeRegistry;
import com.avrinbai.wechatshare.WechatShareCardKind;
import com.avrinbai.wechatshare.extension.WechatShareVisit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.ReactiveExtensionClient;

@Service
public class WechatShareVisitCardKindBackfill {

    private static final Logger log = LoggerFactory.getLogger(WechatShareVisitCardKindBackfill.class);

    private static final int BATCH = 300;

    private final ReactiveExtensionClient client;
    private final ExtensionSchemeRegistry extensionSchemeRegistry;
    private final WechatShareCardService cardService;

    public WechatShareVisitCardKindBackfill(
        ReactiveExtensionClient client,
        ExtensionSchemeRegistry extensionSchemeRegistry,
        WechatShareCardService cardService
    ) {
        this.client = client;
        this.extensionSchemeRegistry = extensionSchemeRegistry;
        this.cardService = cardService;
    }

    public void runAsyncAfterStart() {
        Mono.fromRunnable(this::runBlocking)
            .subscribeOn(Schedulers.boundedElastic())
            .doOnError(ex -> log.warn("visit cardKind backfill failed: {}", ex.toString()))
            .onErrorResume(ex -> Mono.empty())
            .subscribe();
    }

    private void runBlocking() {
        extensionSchemeRegistry.ensureRegistered();
        var sort = Sort.by(Sort.Direction.DESC, "spec.visitedAt");
        int page = 0;
        long updated = 0L;
        while (true) {
            var pageReq = PageRequestImpl.of(page, BATCH, sort);
            ListResult<WechatShareVisit> result =
                client.listBy(WechatShareVisit.class, ListOptions.builder().build(), pageReq)
                    .blockOptional()
                    .orElse(null);
            if (result == null || result.getItems().isEmpty()) {
                break;
            }
            for (var visit : result.getItems()) {
                if (visit == null || visit.getSpec() == null) {
                    continue;
                }
                var sp = visit.getSpec();
                var rawKind = sp.getCardKind();
                if (rawKind != null && !rawKind.isBlank()) {
                    continue;
                }
                var sid = sp.getSid();
                if (sid == null || sid.isBlank()) {
                    continue;
                }
                String kind = WechatShareCardKind.LINK;
                var cardOpt = cardService.findBySid(sid.trim());
                if (cardOpt.isPresent() && cardOpt.get().getSpec() != null) {
                    kind = WechatShareCardKind.normalize(cardOpt.get().getSpec().getCardKind());
                }
                sp.setCardKind(kind);
                client.update(visit).block();
                updated++;
            }
            if (result.getItems().size() < BATCH) {
                break;
            }
            page++;
        }
        if (updated > 0) {
            log.info("Backfilled spec.cardKind on {} WechatShareVisit record(s)", updated);
        }
    }
}
