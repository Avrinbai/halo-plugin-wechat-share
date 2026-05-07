package com.avrinbai.wechatshare.service;

import com.avrinbai.wechatshare.ExtensionSchemeRegistry;
import com.avrinbai.wechatshare.WechatShareCardKind;
import com.avrinbai.wechatshare.api.CardWriteRequest;
import com.avrinbai.wechatshare.api.CardWriteRequest.FileNoteWrite;
import com.avrinbai.wechatshare.extension.WechatShareCard;
import com.avrinbai.wechatshare.support.HttpUrls;
import com.avrinbai.wechatshare.support.PublicUrls;
import com.avrinbai.wechatshare.support.ShareLinks;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.index.query.QueryFactory;

@Service
public class WechatShareCardService {

    private static final Logger log = LoggerFactory.getLogger(WechatShareCardService.class);

    private static final int MAX_LINK_TEXT = 32;
    private static final int MAX_TITLE = 128;
    private static final int MAX_BODY = 512;
    private static final int MAX_URL = 2048;
    private static final int MAX_CONTACT = 512;
    private static final int MAX_FILE_NOTE_TITLE = 128;
    private static final int MAX_FILE_NOTE_DETAIL = 512;
    private static final int MAX_FILE_NOTES = 20;
    private static final int MAX_VIDEO_EXTRA_LINK_LABEL = 64;

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
        var trimmed = sid.trim();
        var options = ListOptions.builder()
            .fieldQuery(QueryFactory.equal("spec.sid", trimmed))
            .build();
        return client.listAll(WechatShareCard.class, options, Sort.unsorted()).next().blockOptional();
    }

    /** 兼容旧接口：纯链接卡片。 */
    public WechatShareCard create(String title, String description, String img, String redirectUrl) {
        return create(new CardWriteRequest(
            WechatShareCardKind.LINK,
            title,
            description,
            img,
            redirectUrl,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of(),
            null
        ));
    }

    public WechatShareCard create(CardWriteRequest req) {
        extensionSchemeRegistry.ensureRegistered();
        var kind = WechatShareCardKind.normalize(req.cardKind());
        validateWrite(kind, req, true);

        for (var i = 0; i < 60; i++) {
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
            applyKindFields(kind, spec, req);
            spec.setEnabled(Boolean.TRUE);
            card.setSpec(spec);

            var created = Objects.requireNonNull(client.create(card).block(), "created card");
            tryAttachShareQrCode(created);
            return client.fetch(WechatShareCard.class, created.getMetadata().getName()).blockOptional().orElse(created);
        }
        throw new IllegalStateException("无法生成唯一 sid，请稍后重试");
    }

    /** 兼容旧接口 */
    public WechatShareCard update(String metadataName, String title, String description, String img, String redirectUrl) {
        return update(metadataName, CardWriteRequest.legacyLink(title, description, img, redirectUrl));
    }

    public WechatShareCard update(String metadataName, CardWriteRequest req) {
        extensionSchemeRegistry.ensureRegistered();
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
        var existingKind = WechatShareCardKind.normalize(card.getSpec().getCardKind());
        var kind = req.cardKind() == null || req.cardKind().isBlank()
            ? existingKind
            : WechatShareCardKind.normalize(req.cardKind());
        validateWrite(kind, req, false);

        var spec = card.getSpec();
        applyKindFields(kind, spec, req);
        if (req.enabled() != null) {
            spec.setEnabled(Boolean.TRUE.equals(req.enabled()));
        }
        spec.setShareQrcodeBase64(null);
        spec.setShareQrcodeMimeType(null);
        client.update(card).block();
        var updated = client.fetch(WechatShareCard.class, name).blockOptional().orElse(card);
        tryAttachShareQrCode(updated);
        return client.fetch(WechatShareCard.class, name).blockOptional().orElse(updated);
    }

    private void applyKindFields(String kind, WechatShareCard.Spec spec, CardWriteRequest req) {
        spec.setCardKind(kind);
        spec.setTitle(trimToEmpty(req.title()));
        spec.setDescription(trimToEmpty(req.description()));
        spec.setImg(HttpUrls.normalize(trimToEmpty(req.img())));
        spec.setRedirectUrl(HttpUrls.normalize(trimToEmpty(resolveRedirectForKind(kind, req))));
        spec.setMediaUrl(emptyToNull(HttpUrls.normalizeOrNull(req.mediaUrl())));
        spec.setDisplayName(emptyToNull(trimToEmpty(req.displayName())));
        if (WechatShareCardKind.FILE.equals(kind)) {
            spec.setFileNotes(normalizeFileNotes(req.fileNotes()));
            spec.setOptionalLinkLabel(null);
            spec.setOptionalLinkUrl(null);
        } else if (WechatShareCardKind.IMAGE.equals(kind)) {
            spec.setFileNotes(normalizeFileNotes(req.fileNotes()));
            spec.setOptionalLinkLabel(null);
            spec.setOptionalLinkUrl(null);
            var imgHeadline = firstNonBlank(trimToEmpty(req.displayName()), trimToEmpty(req.title()));
            spec.setTitle(imgHeadline);
            spec.setDisplayName(imgHeadline.isBlank() ? null : imgHeadline);
        } else if (WechatShareCardKind.AUDIO.equals(kind)) {
            spec.setFileNotes(null);
            spec.setOptionalLinkLabel(null);
            spec.setOptionalLinkUrl(null);
            var headline = firstNonBlank(trimToEmpty(req.displayName()), trimToEmpty(req.title()));
            spec.setTitle(headline);
            spec.setDisplayName(headline.isBlank() ? null : headline);
        } else {
            spec.setFileNotes(null);
            spec.setOptionalLinkLabel(emptyToNull(trimToEmpty(req.optionalLinkLabel())));
            spec.setOptionalLinkUrl(HttpUrls.normalizeOrNull(req.optionalLinkUrl()));
        }
        spec.setContactInfo(emptyToNull(trimToEmpty(req.contactInfo())));
        spec.setVideoTitle(emptyToNull(trimToEmpty(req.videoTitle())));
        spec.setVideoGuideText(emptyToNull(trimToEmpty(req.videoGuideText())));
        spec.setVideoExtraLink(HttpUrls.normalizeOrNull(req.videoExtraLink()));
        spec.setVideoExtraLinkLabel(emptyToNull(trimToEmpty(req.videoExtraLinkLabel())));
        if (WechatShareCardKind.VIDEO.equals(kind)) {
            var headline = firstNonBlank(trimToEmpty(req.videoTitle()), trimToEmpty(req.title()));
            spec.setTitle(headline);
            spec.setVideoTitle(headline.isBlank() ? null : headline);
            if (spec.getVideoExtraLink() == null || spec.getVideoExtraLink().isBlank()) {
                spec.setVideoExtraLinkLabel(null);
            }
        }

        spec.setVideoPasswordHash(null);
    }

    private static String resolveRedirectForKind(String kind, CardWriteRequest req) {
        var direct = trimToEmpty(req.redirectUrl());
        return switch (kind) {
            case WechatShareCardKind.VIDEO -> firstNonBlank(
                direct,
                trimToEmpty(req.videoExtraLink()),
                trimToEmpty(req.mediaUrl())
            );
            case WechatShareCardKind.FILE -> firstNonBlank(direct, trimToEmpty(req.mediaUrl()));
            case WechatShareCardKind.IMAGE -> firstNonBlank(direct, trimToEmpty(req.img()));
            case WechatShareCardKind.AUDIO -> firstNonBlank(direct, trimToEmpty(req.mediaUrl()));
            default -> direct;
        };
    }

    private static String firstNonBlank(String a, String b, String c) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return c == null ? "" : c;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b == null ? "" : b;
    }

    private void validateWrite(String kind, CardWriteRequest req, boolean isCreate) {
        switch (kind) {
            case WechatShareCardKind.LINK -> validateLink(req);
            case WechatShareCardKind.IMAGE -> validateImage(req);
            case WechatShareCardKind.AUDIO -> validateAudio(req);
            case WechatShareCardKind.VIDEO -> validateVideo(req);
            case WechatShareCardKind.FILE -> validateFile(req);
            default -> validateLink(req);
        }
    }

    private void validateLink(CardWriteRequest req) {
        requireText(req.title(), "标题不能为空", MAX_LINK_TEXT, "标题长度不能超过 32 个字符");
        requireText(req.description(), "摘要不能为空", MAX_LINK_TEXT, "摘要长度不能超过 32 个字符");
        requireHttpUrl(req.img(), "封面图不能为空", "封面图必须是 http/https 链接");
        requireHttpUrl(req.redirectUrl(), "跳转链接不能为空", "跳转链接必须是 http/https 链接");
    }

    private void validateImage(CardWriteRequest req) {
        var headline = firstNonBlank(trimToEmpty(req.displayName()), trimToEmpty(req.title()));
        requireText(headline, "页面标题 / 图片名称 / 卡片标题不能为空", MAX_TITLE, "标题过长");
        requireText(req.description(), "图片介绍不能为空", MAX_BODY, "图片介绍过长");
        requireHttpUrl(req.img(), "图片资源不能为空", "图片资源必须是 http/https 链接");
        var go = resolveRedirectForKind(WechatShareCardKind.IMAGE, req);
        if (go.isBlank()) {
            throw new IllegalArgumentException("图片资源不能为空");
        }
        assertHttpUrl(HttpUrls.normalize(go), "跳转目标必须是 http/https 链接");
        validateFileNotes(req.fileNotes());
        validateOptionalContact(req.contactInfo());
    }

    private void validateAudio(CardWriteRequest req) {
        var headline = firstNonBlank(trimToEmpty(req.displayName()), trimToEmpty(req.title()));
        requireText(headline, "页面标题 / 音频名称 / 卡片标题不能为空", MAX_TITLE, "标题过长");
        requireText(req.description(), "音乐介绍不能为空", MAX_BODY, "音乐介绍过长");
        requireHttpUrl(req.img(), "音频封面不能为空", "音频封面必须是 http/https 链接");
        requireHttpUrl(req.mediaUrl(), "音频文件不能为空", "音频地址必须是 http/https 链接");
        var go = resolveRedirectForKind(WechatShareCardKind.AUDIO, req);
        if (go.isBlank()) {
            throw new IllegalArgumentException("音频地址不能为空");
        }
        assertHttpUrl(HttpUrls.normalize(go), "跳转目标必须是 http/https 链接");
        validateOptionalContact(req.contactInfo());
    }

    private void validateVideo(CardWriteRequest req) {
        var headline = firstNonBlank(trimToEmpty(req.videoTitle()), trimToEmpty(req.title()));
        requireText(headline, "页面标题 / 视频标题 / 卡片标题不能为空", MAX_TITLE, "标题过长");
        requireText(req.videoGuideText(), "视频简介不能为空", MAX_BODY, "视频简介过长");
        requireHttpUrl(req.img(), "视频封面不能为空", "视频封面必须是 http/https 链接");
        requireHttpUrl(req.mediaUrl(), "视频文件不能为空", "视频地址必须是 http/https 链接");
        validateOptionalUrl(req.videoExtraLink(), "附加链接格式无效");
        var label = trimToEmpty(req.videoExtraLinkLabel());
        if (label.length() > MAX_VIDEO_EXTRA_LINK_LABEL) {
            throw new IllegalArgumentException("相关链接文案过长");
        }
        var extra = trimToEmpty(req.videoExtraLink());
        if (extra.isBlank() && !label.isBlank()) {
            throw new IllegalArgumentException("填写了相关链接文案时请同时填写附加链接地址");
        }
        var go = resolveRedirectForKind(WechatShareCardKind.VIDEO, req);
        if (go.isBlank()) {
            throw new IllegalArgumentException("请填写附加链接或视频地址至少一项");
        }
        assertHttpUrl(HttpUrls.normalize(go), "可用的跳转目标必须是 http/https 链接");
    }

    private void validateFile(CardWriteRequest req) {
        requireText(req.title(), "页面标题不能为空", MAX_TITLE, "页面标题过长");
        requireText(req.description(), "文件介绍不能为空", MAX_BODY, "文件介绍过长");
        requireHttpUrl(req.img(), "文件封面不能为空", "文件封面必须是 http/https 链接");
        requireHttpUrl(req.mediaUrl(), "文件下载地址不能为空", "文件地址必须是 http/https 链接");
        requireText(req.displayName(), "文件名称不能为空", MAX_TITLE, "文件名称过长");
        var go = resolveRedirectForKind(WechatShareCardKind.FILE, req);
        if (go.isBlank()) {
            throw new IllegalArgumentException("链接地址或下载地址无效");
        }
        assertHttpUrl(HttpUrls.normalize(go), "跳转目标必须是 http/https 链接");
        validateFileNotes(req.fileNotes());
        validateOptionalContact(req.contactInfo());
    }

    private void validateFileNotes(List<FileNoteWrite> raw) {
        if (raw == null || raw.isEmpty()) {
            return;
        }
        if (raw.size() > MAX_FILE_NOTES) {
            throw new IllegalArgumentException("相关说明最多 " + MAX_FILE_NOTES + " 条");
        }
        var idx = 0;
        for (var n : raw) {
            idx++;
            if (n == null) {
                continue;
            }
            var title = trimToEmpty(n.title());
            var detail = trimToEmpty(n.detail());
            var url = trimToEmpty(n.url());
            var jump = Boolean.TRUE.equals(n.jumpLink());
            if (title.isBlank() && detail.isBlank() && (!jump || url.isBlank())) {
                continue;
            }
            if (title.isBlank()) {
                throw new IllegalArgumentException("相关说明第 " + idx + " 条：请填写标题");
            }
            if (title.length() > MAX_FILE_NOTE_TITLE) {
                throw new IllegalArgumentException("相关说明第 " + idx + " 条：标题过长");
            }
            if (detail.length() > MAX_FILE_NOTE_DETAIL) {
                throw new IllegalArgumentException("相关说明第 " + idx + " 条：说明文案过长");
            }
            if (jump) {
                if (url.isBlank()) {
                    throw new IllegalArgumentException("相关说明第 " + idx + " 条：开启跳转时请填写链接地址");
                }
                assertHttpUrl(HttpUrls.normalize(url), "相关说明第 " + idx + " 条：链接必须是 http/https");
            }
        }
    }

    private static List<WechatShareCard.FileNote> normalizeFileNotes(List<FileNoteWrite> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        var out = new java.util.ArrayList<WechatShareCard.FileNote>();
        for (var n : raw) {
            if (n == null) {
                continue;
            }
            var title = trimToEmpty(n.title());
            var detail = trimToEmpty(n.detail());
            var url = trimToEmpty(n.url());
            var jump = Boolean.TRUE.equals(n.jumpLink());
            if (title.isBlank() && detail.isBlank() && (!jump || url.isBlank())) {
                continue;
            }
            var item = new WechatShareCard.FileNote();
            item.setTitle(title.isBlank() ? "说明" : title);
            item.setDetail(detail.isBlank() ? null : detail);
            item.setJumpLink(jump);
            item.setUrl(jump ? HttpUrls.normalizeOrNull(url) : null);
            out.add(item);
        }
        return List.copyOf(out);
    }

    private static void requireText(String v, String emptyMsg, int max, String lenMsg) {
        if (v == null || v.trim().isBlank()) {
            throw new IllegalArgumentException(emptyMsg);
        }
        if (v.trim().length() > max) {
            throw new IllegalArgumentException(lenMsg);
        }
    }

    private static void requireHttpUrl(String v, String emptyMsg, String badMsg) {
        if (v == null || v.trim().isBlank()) {
            throw new IllegalArgumentException(emptyMsg);
        }
        assertHttpUrl(HttpUrls.normalize(v.trim()), badMsg);
    }

    private static void validateOptionalUrl(String v, String badMsg) {
        if (v == null || v.trim().isBlank()) {
            return;
        }
        assertHttpUrl(HttpUrls.normalize(v.trim()), badMsg);
    }

    private static void validateOptionalContact(String v) {
        if (v != null && v.length() > MAX_CONTACT) {
            throw new IllegalArgumentException("联系方式过长");
        }
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

    private static String trimToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    private static String emptyToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s;
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

    public WechatShareCard setEnabled(String metadataName, boolean enabled) {
        extensionSchemeRegistry.ensureRegistered();
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
        card.getSpec().setEnabled(enabled);
        client.update(card).block();
        return client.fetch(WechatShareCard.class, name).blockOptional().orElse(card);
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

    /** 用于微信分享摘要：过长时截断，避免客户端异常。 */
    public static String shareSnippet(String text, int max) {
        if (text == null) {
            return "";
        }
        var t = text.trim();
        if (t.length() <= max) {
            return t;
        }
        return t.substring(0, max);
    }

}
