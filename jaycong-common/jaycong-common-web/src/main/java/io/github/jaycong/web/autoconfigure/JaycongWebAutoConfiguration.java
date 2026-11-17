package io.github.jaycong.web.autoconfigure;

import io.github.jaycong.web.access.RequestLogFilter;
import io.github.jaycong.web.exception.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.DispatcherServlet;

/** 引入 Starter 后自动生效，无需扩大业务应用的组件扫描范围。 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(DispatcherServlet.class)
public class JaycongWebAutoConfiguration {

    /** 默认记录请求摘要；业务可提供同类型 Bean 替换，或通过配置关闭默认实现。 */
    @Bean
    @ConditionalOnMissingBean(RequestLogFilter.class)
    @ConditionalOnProperty(prefix = "jaycong.web.access-log", name = "enabled", matchIfMissing = true)
    public RequestLogFilter requestLogFilter() {
        return new RequestLogFilter();
    }

    /** 在业务 Filter 前建立 requestId，让同步调用链的日志共享 MDC。 */
    @Bean
    @ConditionalOnProperty(prefix = "jaycong.web.access-log", name = "enabled", matchIfMissing = true)
    public FilterRegistrationBean<RequestLogFilter> requestLogFilterRegistration(RequestLogFilter filter) {
        FilterRegistrationBean<RequestLogFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    /** Servlet 模块共享唯一异常入口；应用提供同类型 Bean 时替换默认实现。 */
    @Bean
    @ConditionalOnMissingBean(GlobalExceptionHandler.class)
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
