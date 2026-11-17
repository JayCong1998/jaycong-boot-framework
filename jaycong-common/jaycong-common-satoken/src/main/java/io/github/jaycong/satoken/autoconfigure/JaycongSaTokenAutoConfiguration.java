package io.github.jaycong.satoken.autoconfigure;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jaycong.api.user.client.UserFeignClient;
import io.github.jaycong.satoken.context.AuthContext;
import io.github.jaycong.satoken.context.SaTokenAuthContext;
import io.github.jaycong.satoken.session.AuthSessionKeys;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 仅为 Servlet 应用注册共享登录域和身份入口，保留 sa-token.* 原生配置。
 * 官方 SaBeanInject 将 StpLogic Bean 绑定到 StpUtil；应用可用同类型 Bean 覆盖默认能力。
 * 默认 AuthContext 的角色鉴权复用应用显式启用的 UserFeignClient，未启用时仍可读取会话。
 */
@AutoConfiguration(after = JacksonAutoConfiguration.class)
@ConditionalOnClass(StpUtil.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class JaycongSaTokenAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(StpLogic.class)
    public StpLogic authSessions() {
        return new StpLogic(AuthSessionKeys.LOGIN_TYPE);
    }

    @Bean
    @ConditionalOnMissingBean(AuthContext.class)
    public AuthContext authContext(ObjectProvider<UserFeignClient> users, ObjectMapper json) {
        return new SaTokenAuthContext(users, json);
    }

}
