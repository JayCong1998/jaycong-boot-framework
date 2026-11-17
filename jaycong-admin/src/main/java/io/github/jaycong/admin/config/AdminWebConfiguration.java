package io.github.jaycong.admin.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import io.github.jaycong.admin.service.AdminAccessService;
import io.github.jaycong.api.management.config.AdminFeignConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
public class AdminWebConfiguration implements WebMvcConfigurer {
    private final ObjectProvider<AdminAccessService> access;
    public AdminWebConfiguration(ObjectProvider<AdminAccessService> access) {
        this.access = access;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                access.getObject().requireAdmin();
                // 只透传当前已验证的会话，不复制前端任意身份或内部凭证头。
                request.setAttribute(AdminFeignConfiguration.AUTHORIZATION_ATTRIBUTE, request.getHeader("Authorization"));
                return true;
            }
        }).addPathPatterns("/admin/**");
    }
    /** 库存、状态和版本不得从小数静默截断为整数。 */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer adminStrictIntegerBinding() {
        return builder -> builder.featuresToDisable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
    }
}
