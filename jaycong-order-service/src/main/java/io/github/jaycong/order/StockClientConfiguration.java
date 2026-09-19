package io.github.jaycong.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class StockClientConfiguration {
    @Bean
    StockClient stockClient(RestClient.Builder builder, ObjectMapper json,
            @Value("${stock.base-url}") String baseUrl,
            @Value("${stock.connect-timeout}") Duration connectTimeout,
            @Value("${stock.read-timeout}") Duration readTimeout) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(positiveMillis(connectTimeout));
        factory.setReadTimeout(positiveMillis(readTimeout));
        return new StockClient(builder.baseUrl(baseUrl).requestFactory(factory).build(), json);
    }

    private static int positiveMillis(Duration duration) {
        int millis = Math.toIntExact(duration.toMillis());
        if (millis <= 0) throw new IllegalArgumentException("Stock client timeout must be positive");
        return millis;
    }
}
