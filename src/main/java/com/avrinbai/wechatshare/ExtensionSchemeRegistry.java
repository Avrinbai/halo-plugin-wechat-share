package com.avrinbai.wechatshare;

import static run.halo.app.extension.index.IndexAttributeFactory.simpleAttribute;

import com.avrinbai.wechatshare.extension.WechatShareCard;
import com.avrinbai.wechatshare.extension.WechatShareSettings;
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
    }


    public synchronized void prepareCardSchemeOnStartup() {
        unregisterIfPresent(WechatShareCard.class);
    }

    public synchronized void unregisterOnStop() {
        unregisterIfPresent(WechatShareCard.class);
        unregisterIfPresent(WechatShareSettings.class);
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
