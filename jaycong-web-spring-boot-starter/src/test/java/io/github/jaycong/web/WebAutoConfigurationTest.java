package io.github.jaycong.web;

import io.github.jaycong.web.autoconfigure.JaycongWebAutoConfiguration;
import io.github.jaycong.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class WebAutoConfigurationTest {
    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JaycongWebAutoConfiguration.class));

    @Test void registersDefaultHandlerInServletApplication() {
        runner.run(context -> assertThat(context).hasSingleBean(GlobalExceptionHandler.class));
    }

    @Test void backsOffForUserHandler() {
        runner.withUserConfiguration(CustomConfiguration.class).run(context -> {
            assertThat(context).hasSingleBean(GlobalExceptionHandler.class);
            assertThat(context.getBean(GlobalExceptionHandler.class)).isSameAs(context.getBean("customHandler"));
        });
    }

    @Test void doesNotActivateOutsideServletApplication() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(JaycongWebAutoConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(GlobalExceptionHandler.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomConfiguration {
        @Bean GlobalExceptionHandler customHandler() { return new GlobalExceptionHandler(); }
    }
}
