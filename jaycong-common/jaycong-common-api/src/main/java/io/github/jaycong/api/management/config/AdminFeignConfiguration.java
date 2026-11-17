package io.github.jaycong.api.management.config;

import feign.Logger;
import feign.RequestInterceptor;
import feign.Retryer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.RequestAttributes;

/** 仅管理客户端使用；凭证不记日志，写调用超时后由调用方重新查询确认。 */
public class AdminFeignConfiguration {
    public static final String AUTHORIZATION_ATTRIBUTE = "jaycong.admin.authorization";

    @Bean
    public RequestInterceptor adminCredentials(@Value("${jaycong.rpc.admin.token:}") String token) {
        return template -> {
            if (token.isBlank()) throw new IllegalStateException("管理服务调用凭证未配置");
            var attributes = RequestContextHolder.currentRequestAttributes();
            var authorization = attributes.getAttribute(AUTHORIZATION_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
            if (!(authorization instanceof String value) || value.isBlank())
                throw new IllegalStateException("管理调用缺少已验证会话");
            template.header("X-Admin-Service-Token", token);
            template.header("Authorization", value);
        };
    }
    @Bean public Retryer adminRetryer() { return Retryer.NEVER_RETRY; }
    @Bean public Logger.Level adminLoggerLevel() { return Logger.Level.NONE; }
}
