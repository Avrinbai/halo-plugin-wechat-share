package com.avrinbai.wechatshare.service;

import com.avrinbai.wechatshare.ExtensionSchemeRegistry;
import com.avrinbai.wechatshare.extension.WechatShareVisit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import run.halo.app.extension.ReactiveExtensionClient;

/**
 * 服务端代理解析第三方 IP 归属接口；查询成功后写入 {@link WechatShareVisit#getSpec()}{@code ipLocationText}，
 * 与二维码缓存同属「写入插件扩展」而非浏览器存储。
 */
@Service
public class WechatShareIpLookupService {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final java.net.http.HttpClient HTTP =
        java.net.http.HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
            .build();

    private final WechatShareSettingsService settingsService;
    private final ReactiveExtensionClient client;
    private final ExtensionSchemeRegistry extensionSchemeRegistry;

    public WechatShareIpLookupService(
        WechatShareSettingsService settingsService,
        ReactiveExtensionClient client,
        ExtensionSchemeRegistry extensionSchemeRegistry
    ) {
        this.settingsService = settingsService;
        this.client = client;
        this.extensionSchemeRegistry = extensionSchemeRegistry;
    }

    /**
     * @param visitMetadataName 可选；传入时将命中扩展内已缓存文案，或在成功后写回扩展
     * @return 展示在 IP 后的归属地短文案（不含 IP 本身）
     */
    public String lookup(String rawIp, Optional<String> visitMetadataName) {
        var settings = settingsService.load().blockOptional().orElse(null);
        if (settings == null || settings.getSpec() == null) {
            throw new IllegalStateException("设置不可用");
        }
        var spec = settings.getSpec();
        if (!Boolean.TRUE.equals(spec.getExperimentalIpLookupEnabled())) {
            throw new IllegalStateException("实验功能未开启：请先在插件设置中启用「IP 归属地查询」");
        }
        var ip = normalizeIp(rawIp);
        if (ip.isEmpty()) {
            throw new IllegalArgumentException("IP 为空");
        }
        if (ip.length() > 64) {
            throw new IllegalArgumentException("IP 过长");
        }
        var base = Objects.toString(spec.getIpLookupApiBase(), "").trim();
        if (base.isEmpty()) {
            throw new IllegalStateException("未配置 IP 归属查询接口");
        }

        extensionSchemeRegistry.ensureRegistered();

        Optional<String> visitName =
            visitMetadataName.map(String::trim).filter(s -> !s.isBlank());

        if (visitName.isPresent()) {
            var cached = readCachedFromVisit(visitName.get(), ip);
            if (cached != null) {
                return cached;
            }
        }

        String text = fetchUpstream(base, ip);

        if (visitName.isPresent()) {
            persistToVisit(visitName.get(), ip, text);
        }
        return text;
    }

    private String readCachedFromVisit(String metadataName, String expectedIp) {
        var visit =
            client.fetch(WechatShareVisit.class, metadataName).blockOptional().orElse(null);
        if (visit == null || visit.getSpec() == null) {
            return null;
        }
        var sp = visit.getSpec();
        var storedIp = normalizeIp(sp.getClientIp());
        if (!storedIp.equals(expectedIp)) {
            throw new IllegalArgumentException("访问记录与 IP 不匹配");
        }
        var loc = Objects.toString(sp.getIpLocationText(), "").trim();
        return loc.isEmpty() ? null : loc;
    }

    private void persistToVisit(String metadataName, String expectedIp, String text) {
        var visit =
            client.fetch(WechatShareVisit.class, metadataName).blockOptional().orElse(null);
        if (visit == null || visit.getSpec() == null) {
            return;
        }
        var sp = visit.getSpec();
        if (!normalizeIp(sp.getClientIp()).equals(expectedIp)) {
            return;
        }
        sp.setIpLocationText(text);
        client.update(visit).block();
    }

    private static String fetchUpstream(String base, String ip) {
        URI uri = buildRequestUri(base, ip);
        var req =
            java.net.http.HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            var resp = HTTP.send(req, java.net.http.HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new IllegalStateException("上游返回 HTTP " + resp.statusCode());
            }
            return parseLocationText(resp.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("请求已中断");
        } catch (IOException e) {
            throw new IllegalStateException("请求失败：" + e.getMessage());
        }
    }

    private static URI buildRequestUri(String base, String ip) {
        String enc = URLEncoder.encode(ip, StandardCharsets.UTF_8);
        String u = base.contains("?") ? base + "&ip=" + enc : base + "?ip=" + enc;
        return URI.create(u);
    }

    private static String normalizeIp(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim();
    }

    private static String parseLocationText(String body) {
        if (body == null || body.isBlank()) {
            throw new IllegalStateException("上游返回空内容");
        }
        JsonNode root;
        try {
            root = JSON.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException("上游返回非 JSON");
        }
        if (root.path("code").asInt(-1) != 200) {
            var msg = root.path("msg").asText("请求失败");
            throw new IllegalStateException(msg);
        }
        var data = root.path("data");
        if (data.isMissingNode() || !data.isObject()) {
            throw new IllegalStateException("响应缺少 data");
        }
        if (data.path("status").asInt(-1) != 0) {
            var m = data.path("message").asText("上游定位失败");
            throw new IllegalStateException(m);
        }
        var text = formatAdInfo(data.path("ad_info"));
        if (text.isBlank()) {
            throw new IllegalStateException("无归属地信息");
        }
        return text;
    }

    private static String formatAdInfo(JsonNode ad) {
        if (ad == null || ad.isMissingNode() || !ad.isObject()) {
            return "";
        }
        String nation = text(ad, "nation");
        String province = text(ad, "province");
        String city = text(ad, "city");
        String district = text(ad, "district");
        List<String> merged = new ArrayList<>(4);
        String prev = "";
        for (String s : List.of(nation, province, city, district)) {
            if (!StringUtils.hasText(s)) {
                continue;
            }
            var t = s.trim();
            if (t.equals(prev)) {
                continue;
            }
            merged.add(t);
            prev = t;
        }
        return String.join("", merged);
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode()) {
            return "";
        }
        return node.path(field).asText("").trim();
    }
}
