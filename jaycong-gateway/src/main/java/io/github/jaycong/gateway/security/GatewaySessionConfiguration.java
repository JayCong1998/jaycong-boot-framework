package io.github.jaycong.gateway.security;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 使用与 Auth 相同的 loginType，读取同一 Redis 中的 Authorization 会话。 */
@Configuration(proxyBeanMethods = false)
public class GatewaySessionConfiguration {
    @Bean
    public SaTokenConfig saTokenConfig() {
        SaTokenConfig config = new SaTokenConfig()
                .setTokenName("Authorization")
                .setTokenPrefix("Bearer")
                .setTimeout(86400)
                .setIsReadCookie(false)
                .setIsReadBody(false)
                .setIsReadHeader(false);
        SaManager.setConfig(config);
        return config;
    }

    @Bean
    public StpLogic authSessions() {
        return new StpLogic("jaycong-auth");
    }
}
