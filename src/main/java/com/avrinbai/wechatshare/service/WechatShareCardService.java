package com.avrinbai.wechatshare.service;

import com.avrinbai.wechatshare.ExtensionSchemeRegistry;
import com.avrinbai.wechatshare.extension.WechatShareCard;
import com.avrinbai.wechatshare.support.PublicUrls;
import com.avrinbai.wechatshare.support.ShareLinks;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;

@Service
public class WechatShareCardService {

    private static final Logger log = LoggerFactory.getLogger(WechatShareCardService.class);

    private final ReactiveExtensionClient client;
    private final ExtensionSchemeRegistry extensionSchemeRegistry;
    private final WechatShareSettingsService settingsService;
    private final ShareQrCodeService shareQrCodeService;

    public WechatShareCardService(
        ReactiveExtensionClient client,
        ExtensionSchemeRegistry extensionSchemeRegistry,
        WechatShareSettingsService settingsService,
        ShareQrCodeService shareQrCodeService
    ) {
        this.client = client;
        this.extensionSchemeRegistry = extensionSchemeRegistry;
        this.settingsService = settingsService;
        this.shareQrCodeService = shareQrCodeService;
    }

    public List<WechatShareCard> listAll() {
        extensionSchemeRegistry.ensureRegistered();
        var list = client.listAll(WechatShareCard.class, ListOptions.builder().build(), Sort.unsorted())
            .collectList()
            .blockOptional()
            .orElse(List.of());
        list.sort(Comparator.comparing(WechatShareCardService::creationEpochMillis).reversed());
        return list;
    }

    private static long creationEpochMillis(WechatShareCard c) {
        var ts = c.getMetadata().getCreationTimestamp();
        return ts == null ? 0L : ts.toEpochMilli();
    }

    public Optional<WechatShareCard> findByMetadataName(String metadataName) {
        extensionSchemeRegistry.ensureRegistered();
        if (metadataName == null || metadataName.isBlank()) {
            return Optional.empty();
        }
        return client.fetch(WechatShareCard.class, metadataName.trim()).blockOptional();
    }

    public Optional<WechatShareCard> findBySid(String sid) {
        extensionSchemeRegistry.ensureRegistered();
        if (sid == null || sid.isBlank()) {
            return Optional.empty();
        }
        var found = client.listAll(WechatShareCard.class, ListOptions.builder().build(), Sort.unsorted())
            .filter(c -> c.getSpec() != null && sid.equals(c.getSpec().getSid()))
            .next()
            .blockOptional();
        return found;
    }

    public WechatShareCard create(String title, String description, String img, String redirectUrl) {
        extensionSchemeRegistry.ensureRegistered();
        validate(title, description, img, redirectUrl);

        for (int i = 0; i < 60; i++) {
            var sid = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1_000_000));
            if (findBySid(sid).isPresent()) {
                continue;
            }
            var card = new WechatShareCard();
            var md = new Metadata();
            md.setName(UUID.randomUUID().toString());
            card.setMetadata(md);

            var spec = new WechatShareCard.Spec();
            spec.setSid(sid);
            spec.setTitle(title.trim());
            spec.setDescription(description.trim());
            spec.setImg(normalizeUrl(img.trim()));
            spec.setRedirectUrl(normalizeUrl(redirectUrl.trim()));
            card.setSpec(spec);

            var created = Objects.requireNonNull(client.create(card).block(), "created card");
            tryAttachShareQrCode(created);
            return client.fetch(WechatShareCard.class, created.getMetadata().getName()).blockOptional().orElse(created);
        }
        throw new IllegalStateException("无法生成唯一 sid，请稍后重试");
    }

    public WechatShareCard update(String metadataName, String title, String description, String img, String redirectUrl) {
        extensionSchemeRegistry.ensureRegistered();
        validate(title, description, img, redirectUrl);
        if (metadataName == null || metadataName.isBlank()) {
            throw new IllegalArgumentException("name 不能为空");
        }
        var name = metadataName.trim();
        var card = client.fetch(WechatShareCard.class, name)
            .switchIfEmpty(Mono.error(() -> new IllegalArgumentException("卡片不存在")))
            .block();
        if (card.getSpec() == null) {
            throw new IllegalArgumentException("卡片数据无效");
        }
        var spec = card.getSpec();
        spec.setTitle(title.trim());
        spec.setDescription(description.trim());
        spec.setImg(normalizeUrl(img.trim()));
        spec.setRedirectUrl(normalizeUrl(redirectUrl.trim()));
        spec.setShareQrcodeBase64(null);
        spec.setShareQrcodeMimeType(null);
        client.update(card).block();
        var updated = client.fetch(WechatShareCard.class, name).blockOptional().orElse(card);
        tryAttachShareQrCode(updated);
        return client.fetch(WechatShareCard.class, name).blockOptional().orElse(updated);
    }

    private void tryAttachShareQrCode(WechatShareCard created) {
        try {
            if (created == null || created.getMetadata() == null || created.getSpec() == null) {
                return;
            }
            var settings = settingsService.load().blockOptional().orElse(null);
            if (settings == null || settings.getSpec() == null) {
                return;
            }
            var site = settingsService.resolveExternalSiteUrl(settings);
            var base = WechatShareSettingsService.normalizePath(
                settings.getSpec().getPublicBasePath(),
                WechatShareSettingsService.DEFAULT_PUBLIC_BASE_PATH
            );
            var sid = created.getSpec().getSid();
            var sharePath = ShareLinks.sharePathAndQuery(base, sid);
            var shareUrl = PublicUrls.absoluteHttp(site, sharePath);
            if (shareUrl == null || shareUrl.isBlank()
                || !(shareUrl.startsWith("http://") || shareUrl.startsWith("https://"))) {
                log.debug("Skip QR cache: shareUrl not absolute http(s): {}", shareUrl);
                return;
            }
            var qrBase = settings.getSpec().getQrcodeApiBase();
            if (qrBase == null || qrBase.isBlank()) {
                return;
            }
            var fetched = shareQrCodeService.fetchAsBase64(qrBase.trim(), shareUrl);
            if (fetched.isEmpty()) {
                log.warn("QR upstream returned empty body or failed, sid={}", sid);
                return;
            }
            var result = fetched.get();
            var fresh = client.fetch(WechatShareCard.class, created.getMetadata().getName()).blockOptional().orElse(null);
            if (fresh == null || fresh.getSpec() == null) {
                return;
            }
            fresh.getSpec().setShareQrcodeBase64(result.base64());
            fresh.getSpec().setShareQrcodeMimeType(result.mimeType());
            client.update(fresh).block();
        } catch (Exception ex) {
            log.warn("Failed to cache share QR code: {}", ex.toString());
        }
    }

    public void deleteByName(String metadataName) {
        extensionSchemeRegistry.ensureRegistered();
        if (metadataName == null || metadataName.isBlank()) {
            throw new IllegalArgumentException("name 不能为空");
        }
        var name = metadataName.trim();
        var card = client.fetch(WechatShareCard.class, name)
            .switchIfEmpty(Mono.error(() -> new IllegalArgumentException("卡片不存在")))
            .block();
        client.delete(card).block();
    }

    private static void validate(String title, String description, String img, String redirectUrl) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("摘要不能为空");
        }
        if (img == null || img.isBlank()) {
            throw new IllegalArgumentException("封面图不能为空");
        }
        if (redirectUrl == null || redirectUrl.isBlank()) {
            throw new IllegalArgumentException("跳转链接不能为空");
        }
        if (title.trim().length() > 32 || description.trim().length() > 32) {
            throw new IllegalArgumentException("标题或摘要长度不能超过 32");
        }
        if (img.trim().length() > 2048 || redirectUrl.trim().length() > 2048) {
            throw new IllegalArgumentException("链接过长");
        }
        assertHttpUrl(normalizeUrl(img.trim()), "封面图必须是 http/https 链接");
        assertHttpUrl(normalizeUrl(redirectUrl.trim()), "跳转链接必须是 http/https 链接");
    }

    private static void assertHttpUrl(String url, String message) {
        try {
            var uri = URI.create(url);
            var scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                throw new IllegalArgumentException(message);
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(message);
        }
    }

    private static String normalizeUrl(String url) {
        var u = url.trim();
        if (u.startsWith("//")) {
            return "https:" + u;
        }
        if (!u.matches("(?i)^https?://.*")) {
            return "https://" + u;
        }
        return u;
    }
}
