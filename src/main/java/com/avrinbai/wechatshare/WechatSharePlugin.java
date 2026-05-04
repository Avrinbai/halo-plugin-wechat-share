package com.avrinbai.wechatshare;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;

@Component
public class WechatSharePlugin extends BasePlugin {
    private static final Logger log = LoggerFactory.getLogger(WechatSharePlugin.class);

    private final ExtensionSchemeRegistry extensionSchemeRegistry;

    public WechatSharePlugin(PluginContext pluginContext, ExtensionSchemeRegistry extensionSchemeRegistry) {
        super(pluginContext);
        this.extensionSchemeRegistry = extensionSchemeRegistry;
    }

    @Override
    public void start() {
        extensionSchemeRegistry.ensureRegistered();
        log.info("Plugin wechat-share started");
    }

    @Override
    public void stop() {
        log.info("Plugin wechat-share stopped");
    }
}
