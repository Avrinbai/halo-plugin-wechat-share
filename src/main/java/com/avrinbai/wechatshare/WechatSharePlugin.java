package com.avrinbai.wechatshare;

import com.avrinbai.wechatshare.extension.WechatShareCard;
import com.avrinbai.wechatshare.extension.WechatShareSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;

@Component
public class WechatSharePlugin extends BasePlugin {
    private static final Logger log = LoggerFactory.getLogger(WechatSharePlugin.class);

    private static final String SETTINGS_METADATA_NAME = "default";

    private final ExtensionSchemeRegistry extensionSchemeRegistry;
    private final ReactiveExtensionClient extensionClient;

    public WechatSharePlugin(
        PluginContext pluginContext,
        ExtensionSchemeRegistry extensionSchemeRegistry,
        ReactiveExtensionClient extensionClient
    ) {
        super(pluginContext);
        this.extensionSchemeRegistry = extensionSchemeRegistry;
        this.extensionClient = extensionClient;
    }

    @Override
    public void start() {
        extensionSchemeRegistry.prepareCardSchemeOnStartup();
        extensionSchemeRegistry.ensureRegistered();
        warmUpExtensionIndices();
        log.info("Plugin wechat-share started");
    }

    @Override
    public void stop() {
        extensionSchemeRegistry.unregisterOnStop();
        log.info("Plugin wechat-share stopped");
    }

    private void warmUpExtensionIndices() {
        try {
            extensionClient
                .listAll(WechatShareCard.class, ListOptions.builder().build(), Sort.unsorted())
                .take(1)
                .collectList()
                .block();
            extensionClient.fetch(WechatShareSettings.class, SETTINGS_METADATA_NAME).blockOptional();
        } catch (Exception ex) {
            log.warn("Extension index warm-up skipped: {}", ex.toString());
        }
    }
}
