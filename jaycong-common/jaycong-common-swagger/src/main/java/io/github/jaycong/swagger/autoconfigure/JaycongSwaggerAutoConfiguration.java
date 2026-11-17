package io.github.jaycong.swagger.autoconfigure;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 仅对 Servlet 服务生效；默认提供文档元信息和 Bearer 方案，业务 OpenAPI Bean 可整体替换。
 * 安全方案只声明认证方式，具体接口通过 @SecurityRequirement 标记，避免登录接口要求 Token。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(OpenAPI.class)
@ConditionalOnProperty(prefix = "springdoc.api-docs", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(JaycongSwaggerProperties.class)
public class JaycongSwaggerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI jaycongOpenAPI(JaycongSwaggerProperties properties) {
        return new OpenAPI()
                .info(new Info().title(properties.getTitle()).description(properties.getDescription())
                        .version(properties.getVersion()))
                // 相对根路径沿用当前 UI 的来源，直连与经网关调试都不绑定固定主机或端口。
                .servers(List.of(new Server().url(properties.getServerUrl())))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer")
                        .description("填写登录返回的 tokenValue，无需添加 Bearer 前缀")));
    }
}
