package io.github.jaycong.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import io.github.jaycong.api.user.client.UserFeignClient;

/** 认证接口独立入口，通过 User Facade 获取用户能力，Sa-Token 会话存储于 Redis。 */
@SpringBootApplication
@EnableFeignClients(clients = UserFeignClient.class)
public class AuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
