package com.avrinbai.wechatshare.web;

import com.avrinbai.wechatshare.service.WechatShareSettingsService;
import com.avrinbai.wechatshare.support.ShareRoutePaths;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import run.halo.app.security.AdditionalWebFilter;

@Component
public class WechatSharePublicWebFilter implements AdditionalWebFilter {

    private static final int FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 1000;

    private final WechatShareSiteHandler wechatShareSiteHandler;
    private final WechatShareSettingsService settingsService;
    private final HandlerStrategies handlerStrategies = HandlerStrategies.withDefaults();

    public WechatSharePublicWebFilter(WechatShareSiteHandler wechatShareSiteHandler, WechatShareSettingsService settingsService) {
        this.wechatShareSiteHandler = wechatShareSiteHandler;
        this.settingsService = settingsService;
    }

    @Override
    public int getOrder() {
        return FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        var method = exchange.getRequest().getMethod();
        if (method == null) {
            return chain.filter(exchange);
        }
        var path = stripTrailingSlash(exchange.getRequest().getPath().pathWithinApplication().value());
        return settingsService.load()
            .flatMap(settings -> {
                var base = WechatShareSettingsService.normalizePath(
                    settings.getSpec() == null ? null : settings.getSpec().getPublicBasePath(),
                    WechatShareSettingsService.DEFAULT_PUBLIC_BASE_PATH
                );
                if (!HttpMethod.GET.equals(method)) {
                    return chain.filter(exchange);
                }

                boolean isShare = ShareRoutePaths.matchesShare(path, base);
                boolean isGo = ShareRoutePaths.matchesGo(path, base);
                boolean isView = ShareRoutePaths.matchesView(path, base);
                if (!isShare && !isGo && !isView) {
                    return chain.filter(exchange);
                }
                var req = ServerRequest.create(exchange, handlerStrategies.messageReaders());
                Mono<ServerResponse> responseMono;
                if (isShare) {
                    responseMono = wechatShareSiteHandler.share(req, base);
                } else if (isView) {
                    responseMono = wechatShareSiteHandler.view(req, base);
                } else {
                    responseMono = wechatShareSiteHandler.go(req, base);
                }
                var ctx = new FilterServerResponseContext(handlerStrategies);
                return responseMono.flatMap(sr -> sr.writeTo(exchange, ctx));
            })
            .onErrorResume(ex -> chain.filter(exchange));
    }

    private static String stripTrailingSlash(String path) {
        if (path.length() > 1 && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    private static final class FilterServerResponseContext implements ServerResponse.Context {

        private final HandlerStrategies strategies;

        FilterServerResponseContext(HandlerStrategies strategies) {
            this.strategies = strategies;
        }

        @Override
        public List<HttpMessageWriter<?>> messageWriters() {
            return strategies.messageWriters();
        }

        @Override
        public List<ViewResolver> viewResolvers() {
            return strategies.viewResolvers();
        }
    }
}
