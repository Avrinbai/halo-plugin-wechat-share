package com.avrinbai.wechatshare.service;

import com.avrinbai.wechatshare.WechatShareConstants;
import com.avrinbai.wechatshare.extension.WechatShareSettings;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.util.Objects;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.infra.ExternalUrlSupplier;

@Service
public class WechatShareSettingsService {

    /** @see WechatShareConstants#DEFAULT_PUBLIC_BASE_PATH */
    public static final String DEFAULT_PUBLIC_BASE_PATH = WechatShareConstants.DEFAULT_PUBLIC_BASE_PATH;

    private static final String SETTINGS_NAME = "default";

    private final ReactiveExtensionClient client;
    private final ExternalUrlSupplier externalUrlSupplier;

    public WechatShareSettingsService(ReactiveExtensionClient client, ExternalUrlSupplier externalUrlSupplier) {
        this.client = client;
        this.externalUrlSupplier = externalUrlSupplier;
    }

    public Mono<WechatShareSettings> load() {
        return client.fetch(WechatShareSettings.class, SETTINGS_NAME)
            .switchIfEmpty(Mono.defer(this::createDefault))
            .map(this::stripPublicSiteUrlForApi);
    }

    public Mono<WechatShareSettings> save(WechatShareSettings settings) {
        Objects.requireNonNull(settings, "settings");
        if (settings.getSpec() == null) {
            settings.setSpec(new WechatShareSettings.Spec());
        }
        normalizeSpec(settings.getSpec());
        return client.fetch(WechatShareSettings.class, SETTINGS_NAME)
            .flatMap(existing -> {
                settings.setMetadata(existing.getMetadata());
                settings.getMetadata().setName(SETTINGS_NAME);
                return client.update(settings);
            })
            .switchIfEmpty(Mono.defer(() -> {
                if (settings.getMetadata() == null) {
                    settings.setMetadata(new Metadata());
                }
                settings.getMetadata().setName(SETTINGS_NAME);
                return client.create(settings);
            }))
            .map(this::stripPublicSiteUrlForApi);
    }

    private Mono<WechatShareSettings> createDefault() {
        var s = new WechatShareSettings();
        var md = new Metadata();
        md.setName(SETTINGS_NAME);
        s.setMetadata(md);
        var spec = new WechatShareSettings.Spec();
        normalizeSpec(spec);
        s.setSpec(spec);
        return client.create(s);
    }

    public String resolveExternalSiteUrl(WechatShareSettings ignored) {
        return resolveExternalSiteUrl();
    }

    public String resolveExternalSiteUrl() {
        try {
            var raw = externalUrlSupplier.getRaw();
            if (raw == null) {
                return "";
            }
            var s = externalUrlToString(raw);
            if (s == null || s.isBlank()) {
                return "";
            }
            return normalizeExternalSiteUrlString(s.trim());
        } catch (Exception e) {
            return "";
        }
    }

    private WechatShareSettings stripPublicSiteUrlForApi(WechatShareSettings settings) {
        if (settings != null && settings.getSpec() != null) {
            settings.getSpec().setPublicSiteUrl(null);
        }
        return settings;
    }

    /** Halo 不同版本中 {@link ExternalUrlSupplier#getRaw()} 可能为 {@link URI} 或 {@link URL}。 */
    private static String externalUrlToString(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof URI uri) {
            return uri.toString();
        }
        if (raw instanceof URL url) {
            return url.toString();
        }
        return String.valueOf(raw);
    }

    private static String normalizeExternalSiteUrlString(String s) {
        var v = s.trim();
        if (v.endsWith("/")) {
            return v.substring(0, v.length() - 1);
        }
        return v;
    }

    private static void normalizeSpec(WechatShareSettings.Spec spec) {
        spec.setPublicBasePath(normalizePath(spec.getPublicBasePath(), DEFAULT_PUBLIC_BASE_PATH));
        spec.setPublicSiteUrl(null);
        if (spec.getQrcodeApiBase() == null || spec.getQrcodeApiBase().isBlank()) {
            spec.setQrcodeApiBase("https://api.avrinbai.cn/api/tools/qrcode");
        } else {
            spec.setQrcodeApiBase(spec.getQrcodeApiBase().trim());
        }
        if (spec.getWxAppId() != null) {
            spec.setWxAppId(spec.getWxAppId().trim());
        }
        if (spec.getWxAppSecret() != null) {
            spec.setWxAppSecret(spec.getWxAppSecret().trim());
        }
    }

    public static String normalizePath(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        var v = raw.trim();
        if (v.startsWith("http://") || v.startsWith("https://")) {
            try {
                v = new URI(v).getPath();
            } catch (URISyntaxException e) {
                return fallback;
            }
        }
        if (v == null || v.isBlank()) {
            return fallback;
        }
        if (!v.startsWith("/")) {
            v = "/" + v;
        }
        if (v.length() > 1 && v.endsWith("/")) {
            v = v.substring(0, v.length() - 1);
        }
        return v;
    }
}
