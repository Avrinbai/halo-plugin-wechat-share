package com.avrinbai.wechatshare.service;

import com.avrinbai.wechatshare.ExtensionSchemeRegistry;
import com.avrinbai.wechatshare.WechatShareCardKind;
import com.avrinbai.wechatshare.WechatShareCardStates;
import com.avrinbai.wechatshare.extension.WechatShareCard;
import com.avrinbai.wechatshare.extension.WechatShareStats;
import com.avrinbai.wechatshare.extension.WechatShareVisit;
import com.avrinbai.wechatshare.support.VisitEnvKind;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.index.query.Query;
import run.halo.app.extension.index.query.QueryFactory;

@Service
public class WechatShareAnalyticsService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int TREND_DAYS = 7;
    /** 排行区在控制台内滚动展示，可与卡片总数对齐浏览 */
    private static final int TOP_CARD_LIMIT = 80;

    private final ReactiveExtensionClient client;
    private final ExtensionSchemeRegistry extensionSchemeRegistry;
    private final WechatShareCardService cardService;
    private final WechatShareStatsService statsService;

    public WechatShareAnalyticsService(
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

    public Summary snapshotSummary() {
        extensionSchemeRegistry.ensureRegistered();
        var cards = cardService.listAll();
        long cardCount = cards.size();
        long enabledCount = cards.stream().filter(WechatShareCardStates::isEnabled).count();
        WechatShareStats stats = statsService.loadMono().blockOptional().orElse(null);
        long totalVisits = 0L;
        long wechat = 0L;
        long wework = 0L;
        long qq = 0L;
        long mobileOther = 0L;
        long desktop = 0L;
        long unknown = 0L;
        Map<String, Long> byDay = Map.of();
        if (stats != null && stats.getSpec() != null) {
            var sp = stats.getSpec();
            totalVisits = nullToZero(sp.getTotalVisits());
            wechat = nullToZero(sp.getWechat());
            wework = nullToZero(sp.getWework());
            qq = nullToZero(sp.getQq());
            mobileOther = nullToZero(sp.getMobileOther());
            desktop = nullToZero(sp.getDesktop());
            unknown = nullToZero(sp.getUnknown());
            if (sp.getVisitsByDay() != null && !sp.getVisitsByDay().isEmpty()) {
                byDay = Map.copyOf(sp.getVisitsByDay());
            }
        }

        var trend = buildTrend(byDay, TREND_DAYS);
        long pvSevenDays = trend.stream().mapToLong(TrendPoint::count).sum();

        var scan = computeVisitScanStats();

        var lastBySid = scan.lastVisitedMillisBySid();
        var top = cards.stream()
            .sorted(Comparator.comparingLong((WechatShareCard c) ->
                    nullToZero(c.getSpec() == null ? null : c.getSpec().getVisitCount()))
                .reversed())
            .limit(TOP_CARD_LIMIT)
            .map(c -> {
                var spec = c.getSpec();
                var sid = spec == null ? "" : Objects.toString(spec.getSid(), "");
                var sidKey = sid.trim();
                var title = spec == null ? "" : Objects.toString(spec.getTitle(), "");
                var vc = spec == null ? 0L : nullToZero(spec.getVisitCount());
                var img = spec == null ? "" : Objects.toString(spec.getImg(), "");
                var cardKind = WechatShareCardKind.normalize(spec == null ? null : spec.getCardKind());
                long lastMs = sidKey.isEmpty() ? 0L : lastBySid.getOrDefault(sidKey, 0L);
                return new TopCardRow(sidKey, title, vc, img, cardKind, lastMs);
            })
            .toList();

        return new Summary(
            cardCount,
            enabledCount,
            totalVisits,
            pvSevenDays,
            scan.ipStats().uvSevenDays(),
            scan.ipStats().uniqueIpAllTime(),
            wechat,
            wework,
            qq,
            mobileOther,
            desktop,
            unknown,
            trend,
            top);
    }

    /**
     * 单次分页扫描访问明细：统计独立 IP / 近七日 UV，并汇总每个 SID 的最后访问时间（毫秒）。
     */
    private VisitScanStats computeVisitScanStats() {
        var zone = ZoneId.systemDefault();
        var today = LocalDate.now(zone);
        long windowStart =
            today.minusDays(TREND_DAYS - 1L).atStartOfDay(zone).toInstant().toEpochMilli();
        long windowEnd = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();

        Set<String> allIps = new HashSet<>();
        Set<String> ipsSevenDays = new HashSet<>();
        Map<String, Long> lastVisitedMillisBySid = new HashMap<>();

        int page = 0;
        final int batch = 500;
        var sort = Sort.by(Sort.Direction.DESC, "spec.visitedAt");

        while (true) {
            var pageReq = PageRequestImpl.of(page, batch, sort);
            ListResult<WechatShareVisit> result =
                client.listBy(WechatShareVisit.class, ListOptions.builder().build(), pageReq)
                    .blockOptional()
                    .orElse(null);
            if (result == null || result.getItems().isEmpty()) {
                break;
            }
            for (var v : result.getItems()) {
                if (v == null || v.getSpec() == null) {
                    continue;
                }
                var sp = v.getSpec();
                var sidKey = Objects.toString(sp.getSid(), "").trim();
                long visitedAt = nullToZero(sp.getVisitedAt());
                if (!sidKey.isEmpty() && visitedAt > 0L) {
                    lastVisitedMillisBySid.merge(sidKey, visitedAt, Long::max);
                }
                var rawIp = Objects.toString(sp.getClientIp(), "").trim();
                if (rawIp.isEmpty()) {
                    continue;
                }
                var nip = normalizeIpToken(rawIp);
                allIps.add(nip);
                if (visitedAt >= windowStart && visitedAt < windowEnd) {
                    ipsSevenDays.add(nip);
                }
            }
            if (result.getItems().size() < batch) {
                break;
            }
            page++;
        }

        var ipStats = new IpStats(ipsSevenDays.size(), allIps.size());
        return new VisitScanStats(ipStats, Map.copyOf(lastVisitedMillisBySid));
    }

    private static String normalizeIpToken(String rawIp) {
        return rawIp.trim().toLowerCase();
    }

    private record IpStats(long uvSevenDays, long uniqueIpAllTime) {
    }

    private record VisitScanStats(IpStats ipStats, Map<String, Long> lastVisitedMillisBySid) {
    }

    public VisitPage listVisits(
        int page,
        int size,
        Optional<String> sidFilter,
        Optional<String> cardKindFilter,
        Optional<Long> visitedAfterMillis,
        Optional<Long> visitedBeforeMillis
    ) {
        extensionSchemeRegistry.ensureRegistered();
        var p = Math.max(0, page);
        var sz = Math.min(Math.max(1, size == 0 ? DEFAULT_PAGE_SIZE : size), MAX_PAGE_SIZE);

        var optionsBuilder = ListOptions.builder();
        var clauses = new ArrayList<Query>();
        sidFilter.map(String::trim).filter(s -> !s.isBlank()).ifPresent(s ->
            clauses.add(QueryFactory.equal("spec.sid", s)));
        cardKindFilter
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .ifPresent(k -> clauses.add(
                QueryFactory.equal("spec.cardKind", canonicalCardKindForFilter(k))));
        visitedAfterMillis
            .filter(ms -> ms > 0)
            .ifPresent(ms -> clauses.add(
                QueryFactory.greaterThanOrEqual("spec.visitedAt", visitAtIndexValue(ms))));
        visitedBeforeMillis
            .filter(ms -> ms > 0)
            .ifPresent(ms -> clauses.add(
                QueryFactory.lessThan("spec.visitedAt", visitAtIndexValue(ms))));
        if (!clauses.isEmpty()) {
            Query combined = clauses.getFirst();
            for (int i = 1; i < clauses.size(); i++) {
                combined = QueryFactory.and(combined, clauses.get(i));
            }
            optionsBuilder.fieldQuery(combined);
        }
        var options = optionsBuilder.build();

        var sort = Sort.by(Sort.Direction.DESC, "spec.visitedAt");
        var pageReq = PageRequestImpl.of(p, sz, sort);

        ListResult<WechatShareVisit> result =
            client.listBy(WechatShareVisit.class, options, pageReq).blockOptional().orElse(null);
        if (result == null) {
            return new VisitPage(p, sz, 0L, List.of());
        }

        var bySid = new HashMap<String, WechatShareCard>();
        for (WechatShareCard c : cardService.listAll()) {
            if (c == null || c.getSpec() == null) {
                continue;
            }
            var key = Objects.toString(c.getSpec().getSid(), "").trim();
            if (!key.isEmpty()) {
                bySid.putIfAbsent(key, c);
            }
        }

        var items = new ArrayList<VisitRow>(result.getItems().size());
        for (var v : result.getItems()) {
            if (v == null || v.getSpec() == null) {
                continue;
            }
            var sp = v.getSpec();
            var sidKey = Objects.toString(sp.getSid(), "").trim();
            var card = sidKey.isEmpty() ? null : bySid.get(sidKey);
            String cardTitle = "";
            String cardImg = "";
            var specKind = Objects.toString(sp.getCardKind(), "").trim();
            String cardKind =
                specKind.isEmpty() ? WechatShareCardKind.LINK : WechatShareCardKind.normalize(specKind);
            if (card != null && card.getSpec() != null) {
                var cs = card.getSpec();
                cardTitle = Objects.toString(cs.getTitle(), "");
                cardImg = Objects.toString(cs.getImg(), "");
                if (specKind.isEmpty()) {
                    cardKind = WechatShareCardKind.normalize(cs.getCardKind());
                }
            }
            items.add(new VisitRow(
                v.getMetadata() == null ? "" : Objects.toString(v.getMetadata().getName(), ""),
                Objects.toString(sp.getSid(), ""),
                Objects.toString(sp.getHitType(), ""),
                Objects.toString(sp.getClientIp(), ""),
                Objects.toString(sp.getUserAgent(), ""),
                Objects.toString(sp.getEnvKind(), ""),
                nullToZero(sp.getVisitedAt()),
                cardTitle,
                cardImg,
                cardKind,
                Objects.toString(sp.getIpLocationText(), "")
            ));
        }

        return new VisitPage(p, sz, result.getTotal(), List.copyOf(items));
    }

    private static List<TrendPoint> buildTrend(Map<String, Long> byDay, int days) {
        var zone = ZoneId.systemDefault();
        var today = LocalDate.now(zone);
        var start = today.minusDays(days - 1L);
        var out = new ArrayList<TrendPoint>(days);
        for (var d = start; !d.isAfter(today); d = d.plusDays(1)) {
            var key = d.toString();
            long n = byDay.getOrDefault(key, 0L);
            out.add(new TrendPoint(key, n));
        }
        return List.copyOf(out);
    }

    private static long nullToZero(Long v) {
        return v == null ? 0L : v;
    }

    private static String visitAtIndexValue(long epochMillis) {
        return String.format("%020d", epochMillis);
    }

    private static String canonicalCardKindForFilter(String raw) {
        String t = raw.trim();
        return switch (t) {
            case "链接" -> WechatShareCardKind.LINK;
            case "图片" -> WechatShareCardKind.IMAGE;
            case "音频" -> WechatShareCardKind.AUDIO;
            case "视频" -> WechatShareCardKind.VIDEO;
            case "文件" -> WechatShareCardKind.FILE;
            default -> WechatShareCardKind.normalize(t);
        };
    }

    public record Summary(
        long cardCount,
        long enabledCount,
        long totalVisits,
        long pvSevenDays,
        long uvSevenDays,
        long uniqueIpAllTime,
        long envWechat,
        long envWework,
        long envQq,
        long envMobileOther,
        long envDesktop,
        long envUnknown,
        List<TrendPoint> trendLastDays,
        List<TopCardRow> topCardsByVisits
    ) {
    }

    public record TrendPoint(String date, long count) {
    }

    public record TopCardRow(
        String sid,
        String title,
        long visitCount,
        String img,
        String cardKind,
        long lastVisitedAtMillis
    ) {
    }

    public record VisitPage(int page, int size, long total, List<VisitRow> items) {
    }

    public record VisitRow(
        String metadataName,
        String sid,
        String hitType,
        String clientIp,
        String userAgent,
        String envKind,
        long visitedAtEpochMillis,
        String cardTitle,
        String cardImg,
        String cardKind,
        String ipLocationText
    ) {
    }

    public static String envLabelZh(String envKind) {
        if (envKind == null || envKind.isBlank()) {
            return "未知";
        }
        return switch (envKind) {
            case VisitEnvKind.WECHAT -> "微信内置浏览器";
            case VisitEnvKind.WEWORK -> "企业微信";
            case VisitEnvKind.QQ -> "手机 QQ";
            case VisitEnvKind.MOBILE_OTHER -> "其他移动浏览器";
            case VisitEnvKind.DESKTOP -> "桌面浏览器";
            case VisitEnvKind.UNKNOWN -> "未知";
            default -> envKind;
        };
    }

    public static String hitLabelZh(String hitType) {
        if (hitType == null || hitType.isBlank()) {
            return "";
        }
        return switch (hitType) {
            case "SHARE" -> "推广落地";
            case "VIEW" -> "分享落地";
            case "GO" -> "跳转";
            default -> hitType;
        };
    }
}
