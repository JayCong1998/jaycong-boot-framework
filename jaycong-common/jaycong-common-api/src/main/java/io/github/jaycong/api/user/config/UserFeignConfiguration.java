package io.github.jaycong.api.user.config;

import feign.Logger;
import feign.RequestInterceptor;
import feign.Retryer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/** 仅作用于 User 客户端；凭证不写日志，注册及登录更新不自动重试。 */
public class UserFeignConfiguration {
    @Bean
    public RequestInterceptor userServiceToken(@Value("${jaycong.rpc.user.token:${jaycong.auth.user-service-token:}}") String token) {
        return template -> template.header("X-User-Service-Token", token);
    }

    @Bean
    public Logger.Level userLoggerLevel() { return Logger.Level.NONE; }

    @Bean
    public Retryer userRetryer() { return Retryer.NEVER_RETRY; }
}
