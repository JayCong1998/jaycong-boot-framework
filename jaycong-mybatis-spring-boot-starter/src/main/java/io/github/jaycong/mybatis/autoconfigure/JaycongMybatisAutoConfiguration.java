package io.github.jaycong.mybatis.autoconfigure;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import io.github.jaycong.core.model.PageRequest;
import io.github.jaycong.mybatis.handler.AuditMetaObjectHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/** 默认 Bean 必须先于 MyBatis-Plus 的 SqlSessionFactory 配置注册。 */
@AutoConfiguration(before = MybatisPlusAutoConfiguration.class)
@ConditionalOnClass({MybatisPlusInterceptor.class, PaginationInnerInterceptor.class})
public class JaycongMybatisAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(MybatisPlusInterceptor.class)
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        var pagination = new PaginationInnerInterceptor();
        pagination.setMaxLimit((long) PageRequest.MAX_SIZE);
        pagination.setOverflow(false);
        var interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }

    @Bean
    @ConditionalOnMissingBean(MetaObjectHandler.class)
    public MetaObjectHandler auditMetaObjectHandler() {
        return new AuditMetaObjectHandler();
    }
}
