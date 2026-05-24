package com.avrinbai.wechatshare.api;

import com.avrinbai.wechatshare.WechatShareConstants;
import com.avrinbai.wechatshare.WechatShareCardKind;
import com.avrinbai.wechatshare.WechatShareCardStates;
import com.avrinbai.wechatshare.extension.WechatShareCard;
import com.avrinbai.wechatshare.extension.WechatShareSettings;
import com.avrinbai.wechatshare.service.WechatShareCardService;
import com.avrinbai.wechatshare.service.WechatShareSettingsService;
import com.avrinbai.wechatshare.support.PublicUrls;
import com.avrinbai.wechatshare.support.ShareLinks;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = WechatShareConstants.ADMIN_API_BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminWechatShareController {

    private static final String DEFAULT_IMAGE_MIME = "image/png";

    private final WechatShareCardService cardService;
    private final WechatShareSettingsService settingsService;

    public AdminWechatShareController(WechatShareCardService cardService, WechatShareSettingsService settingsService) {
        this.cardService = cardService;
        this.settingsService = settingsService;
    }

    @GetMapping("/cards")
    public Envelope<List<CardDto>> listCards() {
        var ctx = resolveLinkContext();
        var out = cardService.listAll().stream().map(c -> toCardDto(c, ctx)).toList();
        return Envelope.ok(out);
    }

    @PostMapping("/cards")
    public Envelope<CardDto> create(@RequestBody CardWriteRequest req) {
        try {
            var card = cardService.create(req);
            return Envelope.ok(toCardDto(card, resolveLinkContext()));
        } catch (IllegalArgumentException ex) {
            return Envelope.error(ex.getMessage());
        } catch (Exception ex) {
            return Envelope.error("创建失败，请稍后重试");
        }
    }

    @PatchMapping(path = "/cards/{metadataName}/enabled", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Envelope<CardDto> patchEnabled(
        @PathVariable("metadataName") String metadataName,
        @RequestBody EnabledPatch body
    ) {
        try {
            var card = cardService.setEnabled(metadataName, body.enabled());
            return Envelope.ok(toCardDto(card, resolveLinkContext()));
        } catch (IllegalArgumentException ex) {
            return Envelope.error(ex.getMessage());
        } catch (Exception ex) {
            return Envelope.error("更新状态失败，请稍后重试");
        }
    }

    @PutMapping("/cards/{metadataName}")
    public Envelope<CardDto> update(@PathVariable("metadataName") String metadataName, @RequestBody CardWriteRequest req) {
        try {
            var card = cardService.update(metadataName, req);
            return Envelope.ok(toCardDto(card, resolveLinkContext()));
        } catch (IllegalArgumentException ex) {
            return Envelope.error(ex.getMessage());
        } catch (Exception ex) {
            return Envelope.error("保存失败，请稍后重试");
        }
    }

    @GetMapping("/cards/{metadataName}/share-qrcode")
    public Envelope<ShareQrPayload> shareQr(@PathVariable("metadataName") String metadataName) {
        var card = cardService.findByMetadataName(metadataName).orElse(null);
        if (card == null || card.getSpec() == null) {
            return Envelope.error("卡片不存在");
        }
        var b64 = card.getSpec().getShareQrcodeBase64();
        if (b64 == null || b64.isBlank()) {
            return Envelope.error("暂无二维码缓存：请确认 Halo「设置 → 外部访问地址」已配置且为 http(s)，保存卡片后系统将自动生成并缓存二维码。");
        }
        var mime = defaultImageMime(card.getSpec().getShareQrcodeMimeType());
        return Envelope.ok(new ShareQrPayload(mime, b64));
    }

    @DeleteMapping("/cards/{metadataName}")
    public Envelope<Map<String, Object>> delete(@PathVariable("metadataName") String metadataName) {
        try {
            cardService.deleteByName(metadataName);
            return Envelope.ok(Map.<String, Object>of("deleted", true));
        } catch (IllegalArgumentException ex) {
            return Envelope.error(ex.getMessage());
        } catch (Exception ex) {
            return Envelope.error("删除失败，请稍后重试");
        }
    }

    @GetMapping("/settings")
    public Envelope<WechatShareSettings> settings() {
        var s = settingsService.load().blockOptional().orElseThrow();
        return Envelope.ok(s);
    }

    @PutMapping("/settings")
    public Envelope<WechatShareSettings> saveSettings(@RequestBody WechatShareSettings incoming) {
        try {
            var saved = settingsService.save(incoming).blockOptional().orElseThrow();
            return Envelope.ok(saved);
        } catch (Exception ex) {
            return Envelope.error("保存失败，请检查配置项");
        }
    }

    private LinkContext resolveLinkContext() {
        var settings = settingsService.load().blockOptional().orElseThrow();
        var site = settingsService.resolveExternalSiteUrl(settings);
        var base = WechatShareSettingsService.normalizePath(
            settings.getSpec() == null ? null : settings.getSpec().getPublicBasePath(),
            WechatShareSettingsService.DEFAULT_PUBLIC_BASE_PATH
        );
        return new LinkContext(site, base);
    }

    private static CardDto toCardDto(WechatShareCard card, LinkContext ctx) {
        var spec = card.getSpec();
        var sid = spec.getSid();
        var sharePath = ShareLinks.sharePathAndQuery(ctx.publicBase(), sid);
        var goPath = ShareLinks.goPathAndQuery(ctx.publicBase(), sid);
        var shareUrl = PublicUrls.absoluteHttp(ctx.siteUrl(), sharePath);
        var goUrl = PublicUrls.absoluteHttp(ctx.siteUrl(), goPath);

        var b64 = spec.getShareQrcodeBase64();
        String shareQrcodeDataUrl = null;
        if (b64 != null && !b64.isBlank()) {
            var mime = defaultImageMime(spec.getShareQrcodeMimeType());
            shareQrcodeDataUrl = "data:" + mime + ";base64," + b64;
        }

        var kind = WechatShareCardKind.normalize(spec.getCardKind());
        var enabled = WechatShareCardStates.isEnabled(card);
        var visitCount = spec.getVisitCount() == null ? 0L : spec.getVisitCount();

        return new CardDto(
            card.getMetadata().getName(),
            sid,
            kind,
            enabled,
            visitCount,
            spec.getTitle(),
            spec.getDescription(),
            spec.getImg(),
            spec.getRedirectUrl(),
            spec.getMediaUrl(),
            spec.getDisplayName(),
            spec.getOptionalLinkLabel(),
            spec.getOptionalLinkUrl(),
            mapFileNotes(spec),
            spec.getContactInfo(),
            spec.getVideoTitle(),
            spec.getVideoGuideText(),
            spec.getVideoExtraLink(),
            spec.getVideoExtraLinkLabel(),
            shareUrl,
            goUrl,
            shareQrcodeDataUrl
        );
    }

    private static List<FileNoteDto> mapFileNotes(WechatShareCard.Spec spec) {
        var list = spec.getFileNotes();
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        var out = new ArrayList<FileNoteDto>(list.size());
        for (var n : list) {
            if (n == null) {
                continue;
            }
            var title = n.getTitle() == null ? "" : n.getTitle();
            var detail = n.getDetail() == null ? "" : n.getDetail();
            var url = n.getUrl() == null ? "" : n.getUrl();
            var jump = Boolean.TRUE.equals(n.getJumpLink());
            out.add(new FileNoteDto(title, detail, jump, url));
        }
        return List.copyOf(out);
    }

    private static String defaultImageMime(String mime) {
        return (mime == null || mime.isBlank()) ? DEFAULT_IMAGE_MIME : mime;
    }

    private record LinkContext(String siteUrl, String publicBase) {
    }

    public record CardDto(
        String metadataName,
        String sid,
        String cardKind,
        boolean enabled,
        long visitCount,
        String title,
        String description,
        String img,
        String redirectUrl,
        String mediaUrl,
        String displayName,
        String optionalLinkLabel,
        String optionalLinkUrl,
        List<FileNoteDto> fileNotes,
        String contactInfo,
        String videoTitle,
        String videoGuideText,
        String videoExtraLink,
        String videoExtraLinkLabel,
        String shareUrl,
        String goUrl,
        String shareQrcodeDataUrl
    ) {
    }

    public record FileNoteDto(String title, String detail, boolean jumpLink, String url) {
    }

    public record ShareQrPayload(String mimeType, String base64) {
    }
}
