package io.github.jaycong.order.infrastructure.config;

import io.github.jaycong.api.goods.client.GoodsFeignClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/** 仅启用 common-api 的商品读取客户端，避免扫描并注册无关远程服务。 */
@Configuration(proxyBeanMethods = false)
@EnableFeignClients(clients = GoodsFeignClient.class)
public class OrderConfiguration {
}
