package io.github.jaycong.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 用户服务独立入口，承载用户领域、内部 Facade 和会员管理，共享 Auth 的 Redis 会话。 */
@SpringBootApplication
public class UserApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }
}
