package com.avrinbai.wechatshare.api;

import com.avrinbai.wechatshare.WechatShareConstants;
import com.avrinbai.wechatshare.service.WechatShareAnalyticsService;
import com.avrinbai.wechatshare.service.WechatShareIpLookupService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = WechatShareConstants.ADMIN_API_BASE_PATH + "/analytics", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminWechatShareAnalyticsController {

    private final WechatShareAnalyticsService analyticsService;
    private final WechatShareIpLookupService ipLookupService;

    public AdminWechatShareAnalyticsController(
        WechatShareAnalyticsService analyticsService,
        WechatShareIpLookupService ipLookupService
    ) {
        this.analyticsService = analyticsService;
        this.ipLookupService = ipLookupService;
    }

    @GetMapping("/summary")
    public Envelope<AnalyticsSummaryDto> summary() {
        var s = analyticsService.snapshotSummary();
        var dto = new AnalyticsSummaryDto(
            s.cardCount(),
            s.enabledCount(),
            s.totalVisits(),
            s.pvSevenDays(),
            s.uvSevenDays(),
            s.uniqueIpAllTime(),
            new EnvBreakdownDto(
                s.envWechat(),
                s.envWework(),
                s.envMobileOther(),
                s.envDesktop(),
                s.envUnknown()
            ),
            s.trendLastDays().stream().map(tp -> new TrendPointDto(tp.date(), tp.count())).toList(),
            s.topCardsByVisits().stream()
                .map(t -> new TopCardDto(
                    t.sid(),
                    t.title(),
                    t.visitCount(),
                    t.img(),
                    t.cardKind(),
                    t.lastVisitedAtMillis()))
                .toList()
        );
        return Envelope.ok(dto);
    }

    @GetMapping("/visits")
    public Envelope<VisitPageDto> visits(
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "20") int size,
        @RequestParam(value = "sid", required = false) String sid,
        @RequestParam(value = "cardKind", required = false) String cardKind,
        @RequestParam(value = "visitedAfter", required = false) Long visitedAfter,
        @RequestParam(value = "visitedBefore", required = false) Long visitedBefore
    ) {
        Optional<String> sidOpt =
            (sid == null || sid.isBlank()) ? Optional.empty() : Optional.of(sid.trim());
        Optional<String> cardKindOpt =
            (cardKind == null || cardKind.isBlank()) ? Optional.empty() : Optional.of(cardKind.trim());
        Optional<Long> visitedAfterOpt = Optional.ofNullable(visitedAfter);
        Optional<Long> visitedBeforeOpt = Optional.ofNullable(visitedBefore);
        var pageResult =
            analyticsService.listVisits(page, size, sidOpt, cardKindOpt, visitedAfterOpt, visitedBeforeOpt);
        var items = pageResult.items().stream().map(v -> new VisitRowDto(
            v.metadataName(),
            v.sid(),
            v.hitType(),
            WechatShareAnalyticsService.hitLabelZh(v.hitType()),
            v.cardTitle(),
            v.cardImg(),
            v.cardKind(),
            v.clientIp(),
            v.userAgent(),
            v.envKind(),
            WechatShareAnalyticsService.envLabelZh(v.envKind()),
            Instant.ofEpochMilli(v.visitedAtEpochMillis()).toString(),
            v.ipLocationText()
        )).toList();
        var dto = new VisitPageDto(pageResult.page(), pageResult.size(), pageResult.total(), items);
        return Envelope.ok(dto);
    }

    @GetMapping("/ip-location")
    public Envelope<IpLocationDto> ipLocation(
        @RequestParam("ip") String ip,
        @RequestParam(value = "visit", required = false) String visit
    ) {
        try {
            Optional<String> visitOpt =
                (visit == null || visit.isBlank()) ? Optional.empty() : Optional.of(visit.trim());
            var text = ipLookupService.lookup(ip, visitOpt);
            return Envelope.ok(new IpLocationDto(text));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return Envelope.error(ex.getMessage());
        } catch (Exception ex) {
            return Envelope.error("查询失败，请稍后重试");
        }
    }

    public record IpLocationDto(String locationText) {
    }

    public record AnalyticsSummaryDto(
        long cardCount,
        long enabledCount,
        long totalVisits,
        long pvSevenDays,
        long uvSevenDays,
        long uniqueIpAllTime,
        EnvBreakdownDto envBreakdown,
        List<TrendPointDto> trendLastDays,
        List<TopCardDto> topCards
    ) {
    }

    public record EnvBreakdownDto(long wechat, long wework, long mobileOther, long desktop, long unknown) {
    }

    public record TrendPointDto(String date, long count) {
    }

    public record TopCardDto(
        String sid,
        String title,
        long visitCount,
        String img,
        String cardKind,
        long lastVisitedAtMillis
    ) {
    }

    public record VisitPageDto(int page, int size, long total, List<VisitRowDto> items) {
    }

    public record VisitRowDto(
        String metadataName,
        String sid,
        String hitType,
        String hitLabel,
        String cardTitle,
        String cardImg,
        String cardKind,
        String clientIp,
        String userAgent,
        String envKind,
        String envLabel,
        String visitedAtIso,
        String ipLocationText
    ) {
    }

}
