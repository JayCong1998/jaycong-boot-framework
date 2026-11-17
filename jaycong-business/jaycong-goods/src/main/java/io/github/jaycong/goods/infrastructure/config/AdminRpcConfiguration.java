package io.github.jaycong.goods.infrastructure.config;

import io.github.jaycong.goods.GoodsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 内部路径仍必须验证服务身份；未配置凭证时拒绝调用，不回退为匿名访问。 */
@Configuration(proxyBeanMethods = false)
public class AdminRpcConfiguration implements WebMvcConfigurer {
    private final String token;
    public AdminRpcConfiguration(@Value("${jaycong.rpc.admin.token:}") String token) { this.token = token; }
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                String supplied = request.getHeader("X-Admin-Service-Token");
                if (token.isBlank() || supplied == null || !MessageDigest.isEqual(
                        token.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8)))
                    throw new GoodsException(403, "内部管理调用未授权");
                return true;
            }
        }).addPathPatterns("/internal/admin/**");
    }
}
