package io.github.jaycong.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import io.github.jaycong.api.user.request.UserAuthenticateRequest;
import io.github.jaycong.api.user.request.UserRegisterRequest;
import io.github.jaycong.api.user.response.UserInfo;
import io.github.jaycong.auth.exception.AuthException;
import io.github.jaycong.api.user.client.UserFeignClient;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** 委托 User 远程能力并转换错误，不在 Auth 重复用户领域规则。 */
@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserFeignClient client;
    private final ObjectMapper json;

    public UserService(UserFeignClient client, ObjectMapper json) {
        this.client = client;
        this.json = json;
    }

    public UserInfo register(UserRegisterRequest request) { return call(() -> client.register(request)); }

    public UserInfo authenticate(UserAuthenticateRequest request) { return call(() -> client.authenticate(request)); }

    public UserInfo findEnabledUser(long id) { return call(() -> client.findEnabledUser(id)); }

    private UserInfo call(Supplier<UserInfo> request) {
        try {
            UserInfo result = request.get();
            if (result == null || result.id() == null || result.id().isBlank()) {
                throw new AuthException(503, "用户服务暂不可用");
            }
            try {
                if (Long.parseLong(result.id()) <= 0) {
                    throw new AuthException(503, "用户服务暂不可用");
                }
            } catch (NumberFormatException exception) {
                // 远程协议异常不能当作客户端参数错误，也不能进入会话签发流程。
                throw new AuthException(503, "用户服务暂不可用");
            }
            return result;
        } catch (FeignException exception) {
            int status = exception.status();
            if (status == 400 || status == 401 || status == 409) {
                try {
                    var body = json.readTree(exception.contentUTF8());
                    if (body != null && body.path("code").asInt() == status
                            && body.path("message").isTextual() && !body.path("message").asText().isBlank()) {
                        throw new AuthException(status, body.path("message").asText());
                    }
                } catch (java.io.IOException ignored) {
                    // 下游非标准响应按服务故障处理，不向客户端暴露远程响应正文。
                }
            }
            log.warn("User RPC 失败 status={} responseBytes={} cause={}", status,
                    exception.contentUTF8().length(),
                    exception.getCause() == null ? "none" : exception.getCause().getClass().getSimpleName());
            throw new AuthException(503, "用户服务暂不可用");
        }
    }
}
