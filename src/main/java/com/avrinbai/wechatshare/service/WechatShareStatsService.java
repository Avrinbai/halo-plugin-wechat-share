package com.avrinbai.wechatshare.service;

import com.avrinbai.wechatshare.ExtensionSchemeRegistry;
import com.avrinbai.wechatshare.extension.WechatShareStats;
import com.avrinbai.wechatshare.support.VisitEnvKind;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Iterator;
import java.util.Map;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;

@Service
public class WechatShareStatsService {

    public static final String STATS_METADATA_NAME = "aggregate";

    private static final int DAY_BUCKET_RETENTION = 120;

    private final ReactiveExtensionClient client;
    private final ExtensionSchemeRegistry extensionSchemeRegistry;

    public WechatShareStatsService(ReactiveExtensionClient client, ExtensionSchemeRegistry extensionSchemeRegistry) {
        this.client = client;
        this.extensionSchemeRegistry = extensionSchemeRegistry;
    }

    public synchronized void increment(String envKind, long visitedAtEpochMillis) {
        extensionSchemeRegistry.ensureRegistered();
        var stats = client.fetch(WechatShareStats.class, STATS_METADATA_NAME).blockOptional().orElse(null);
        if (stats == null || stats.getSpec() == null) {
            stats = newEmptyStats();
            client.create(stats).block();
            stats = client.fetch(WechatShareStats.class, STATS_METADATA_NAME).blockOptional().orElse(stats);
        }
        var spec = stats.getSpec();
        if (spec == null) {
            return;
        }
        spec.setTotalVisits(nullToZero(spec.getTotalVisits()) + 1);
        bumpEnv(spec, envKind);
        var dayKey = LocalDate.ofInstant(Instant.ofEpochMilli(visitedAtEpochMillis), ZoneId.systemDefault()).toString();
        var map = spec.safeVisitsByDay();
        map.merge(dayKey, 1L, Long::sum);
        pruneOldDays(map, DAY_BUCKET_RETENTION);
        client.update(stats).block();
    }

    public Mono<WechatShareStats> loadMono() {
        extensionSchemeRegistry.ensureRegistered();
        return client.fetch(WechatShareStats.class, STATS_METADATA_NAME);
    }

    private static void bumpEnv(WechatShareStats.Spec spec, String envKind) {
        var k = envKind == null ? VisitEnvKind.UNKNOWN : envKind;
        switch (k) {
            case VisitEnvKind.WECHAT -> spec.setWechat(nullToZero(spec.getWechat()) + 1);
            case VisitEnvKind.WEWORK -> spec.setWework(nullToZero(spec.getWework()) + 1);
            case VisitEnvKind.MOBILE_OTHER -> spec.setMobileOther(nullToZero(spec.getMobileOther()) + 1);
            case VisitEnvKind.DESKTOP -> spec.setDesktop(nullToZero(spec.getDesktop()) + 1);
            default -> spec.setUnknown(nullToZero(spec.getUnknown()) + 1);
        }
    }

    private static long nullToZero(Long v) {
        return v == null ? 0L : v;
    }

    private static void pruneOldDays(Map<String, Long> map, int keepMostRecentKeys) {
        if (map.size() <= keepMostRecentKeys) {
            return;
        }
        var keys = map.keySet().stream().sorted().toList();
        int drop = keys.size() - keepMostRecentKeys;
        if (drop <= 0) {
            return;
        }
        Iterator<String> it = keys.iterator();
        int removed = 0;
        while (it.hasNext() && removed < drop) {
            map.remove(it.next());
            removed++;
        }
    }

    private static WechatShareStats newEmptyStats() {
        var s = new WechatShareStats();
        var md = new Metadata();
        md.setName(STATS_METADATA_NAME);
        s.setMetadata(md);
        var spec = new WechatShareStats.Spec();
        spec.setTotalVisits(0L);
        spec.setWechat(0L);
        spec.setWework(0L);
        spec.setMobileOther(0L);
        spec.setDesktop(0L);
        spec.setUnknown(0L);
        spec.safeVisitsByDay();
        s.setSpec(spec);
        return s;
    }
}
