package io.github.jaycong.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import io.github.jaycong.mybatis.autoconfigure.JaycongMybatisAutoConfiguration;
import org.apache.ibatis.reflection.MetaObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class MybatisAutoConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JaycongMybatisAutoConfiguration.class));

    @Test void suppliesDefaults() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(MybatisPlusInterceptor.class).hasSingleBean(MetaObjectHandler.class);
        });
    }

    @Test void customBeansReplaceDefaults() {
        runner.withUserConfiguration(Custom.class).run(context -> {
            assertThat(context).hasSingleBean(MybatisPlusInterceptor.class).hasSingleBean(MetaObjectHandler.class);
            assertThat(context.getBean(MybatisPlusInterceptor.class)).isSameAs(context.getBean("customInterceptor"));
            assertThat(context.getBean(MetaObjectHandler.class)).isSameAs(context.getBean("customHandler"));
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class Custom {
        @Bean MybatisPlusInterceptor customInterceptor() { return new MybatisPlusInterceptor(); }
        @Bean MetaObjectHandler customHandler() {
            return new MetaObjectHandler() {
                public void insertFill(MetaObject object) { }
                public void updateFill(MetaObject object) { }
            };
        }
    }
}
