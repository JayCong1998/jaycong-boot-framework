package io.github.jaycong.gateway.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.http.server.logging.AccessLog;

/** 使用 Netty 完成响应时的访问日志，包含未匹配路由的请求，不启用 wiretap。 */
@Configuration(proxyBeanMethods = false)
public class GatewayAccessLogConfiguration {
    @Bean
    @ConditionalOnProperty(prefix = "jaycong.gateway.access-log", name = "enabled", havingValue = "true", matchIfMissing = true)
    public WebServerFactoryCustomizer<NettyReactiveWebServerFactory> gatewayAccessLogCustomizer() {
        return factory -> factory.addServerCustomizers(server -> server.accessLog(true, args -> {
            String uri = String.valueOf(args.uri());
            int queryStart = uri.indexOf('?');
            String path = (queryStart < 0 ? uri : uri.substring(0, queryStart)).replaceAll("[\\p{Cntrl}]", "_");
            return AccessLog.create("event=http_access method={} path={} status={} durationMs={}",
                    args.method(), path, args.status(), args.duration());
        }));
    }
}
