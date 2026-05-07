package com.avrinbai.wechatshare;

import static run.halo.app.extension.index.IndexAttributeFactory.simpleAttribute;

import com.avrinbai.wechatshare.extension.WechatShareCard;
import com.avrinbai.wechatshare.extension.WechatShareSettings;
import com.avrinbai.wechatshare.extension.WechatShareStats;
import com.avrinbai.wechatshare.WechatShareCardKind;
import com.avrinbai.wechatshare.extension.WechatShareVisit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import run.halo.app.extension.Extension;
import run.halo.app.extension.Scheme;
import run.halo.app.extension.SchemeManager;
import run.halo.app.extension.index.IndexSpec;

@Component
@SuppressWarnings("removal")
public class ExtensionSchemeRegistry {
    private static final Logger log = LoggerFactory.getLogger(ExtensionSchemeRegistry.class);

    private final SchemeManager schemeManager;

    public ExtensionSchemeRegistry(SchemeManager schemeManager) {
        this.schemeManager = schemeManager;
    }

    public synchronized void ensureRegistered() {
        ensureWechatShareCard();
        ensureWechatShareSettings();
        ensureWechatShareVisit();
        ensureWechatShareStats();
    }


    public synchronized void prepareCardSchemeOnStartup() {
        unregisterIfPresent(WechatShareCard.class);
        unregisterIfPresent(WechatShareVisit.class);
    }

    public synchronized void unregisterOnStop() {
        unregisterIfPresent(WechatShareCard.class);
        unregisterIfPresent(WechatShareSettings.class);
        unregisterIfPresent(WechatShareVisit.class);
        unregisterIfPresent(WechatShareStats.class);
    }

    private void ensureWechatShareCard() {
        if (hasScheme(WechatShareCard.class)) {
            return;
        }
        schemeManager.register(WechatShareCard.class, indexSpecs -> indexSpecs.add(new IndexSpec()
            .setName("spec.sid")
            .setIndexFunc(simpleAttribute(WechatShareCard.class, x ->
                x.getSpec() == null ? null : x.getSpec().getSid()))));
        log.debug("Registered extension scheme: {}", WechatShareCard.class.getSimpleName());
    }

    private void ensureWechatShareSettings() {
        if (hasScheme(WechatShareSettings.class)) {
            return;
        }
        schemeManager.register(WechatShareSettings.class);
        log.debug("Registered extension scheme: {}", WechatShareSettings.class.getSimpleName());
    }

    private void ensureWechatShareVisit() {
        if (hasScheme(WechatShareVisit.class)) {
            return;
        }
        schemeManager.register(WechatShareVisit.class, indexSpecs -> {
            indexSpecs.add(new IndexSpec()
                .setName("spec.sid")
                .setIndexFunc(simpleAttribute(WechatShareVisit.class, x ->
                    x.getSpec() == null ? null : x.getSpec().getSid())));
            indexSpecs.add(new IndexSpec()
                .setName("spec.visitedAt")
                .setIndexFunc(simpleAttribute(WechatShareVisit.class, x -> {
                    if (x.getSpec() == null || x.getSpec().getVisitedAt() == null) {
                        return null;
                    }
                    return String.format("%020d", x.getSpec().getVisitedAt());
                })));
            indexSpecs.add(new IndexSpec()
                .setName("spec.cardKind")
                .setIndexFunc(simpleAttribute(WechatShareVisit.class, x -> {
                    if (x.getSpec() == null) {
                        return null;
                    }
                    var raw = x.getSpec().getCardKind();
                    if (raw == null || raw.isBlank()) {
                        return WechatShareCardKind.LINK;
                    }
                    return WechatShareCardKind.normalize(raw);
                })));
        });
        log.debug("Registered extension scheme: {}", WechatShareVisit.class.getSimpleName());
    }

    private void ensureWechatShareStats() {
        if (hasScheme(WechatShareStats.class)) {
            return;
        }
        schemeManager.register(WechatShareStats.class);
        log.debug("Registered extension scheme: {}", WechatShareStats.class.getSimpleName());
    }

    private boolean hasScheme(Class<? extends Extension> type) {
        try {
            return schemeManager.get(type) != null;
        } catch (Exception ex) {
            return false;
        }
    }

    private void unregisterIfPresent(Class<? extends Extension> type) {
        try {
            Scheme scheme = schemeManager.get(type);
            if (scheme != null) {
                schemeManager.unregister(scheme);
                log.info("Unregistered extension scheme: {}", type.getName());
            }
        } catch (Exception ex) {
            log.debug("Skip unregister for {} due to: {}", type.getName(), ex.getMessage());
        }
    }
}
