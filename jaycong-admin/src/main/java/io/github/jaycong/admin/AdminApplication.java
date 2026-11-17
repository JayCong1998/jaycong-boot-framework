package io.github.jaycong.admin;

import io.github.jaycong.api.user.client.UserFeignClient;
import io.github.jaycong.api.user.management.client.MemberManagementClient;
import io.github.jaycong.api.user.management.client.MemberOperationManagementClient;
import io.github.jaycong.api.goods.management.client.GoodsManagementClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/** 管理端独立入口，通过公共 RPC 契约调用业务服务。 */
@SpringBootApplication
@EnableFeignClients(clients = {UserFeignClient.class, MemberManagementClient.class,
        MemberOperationManagementClient.class, GoodsManagementClient.class})
public class AdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}
