package io.github.jaycong.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import io.github.jaycong.admin.exception.AdminException;
import java.util.Set;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** 管理 RPC 的错误转换集中在 Service，Web 异常处理器不依赖 Feign。 */
@Service
public class AdminRemoteService {
    private static final Logger log = LoggerFactory.getLogger(AdminRemoteService.class);
    private final ObjectMapper json;

    public AdminRemoteService(ObjectMapper json) { this.json = json; }

    public <T> T call(Supplier<T> request) {
        return invoke(() -> {
            T result = request.get();
            if (result == null) throw unavailable();
            return result;
        });
    }

    public void run(Runnable request) {
        invoke(() -> { request.run(); return null; });
    }

    private <T> T invoke(Supplier<T> request) {
        try {
            return request.get();
        } catch (FeignException exception) {
            int status = exception.status();
            if (Set.of(400, 401, 403, 404, 409, 422).contains(status)) {
                try {
                    var body = json.readTree(exception.contentUTF8());
                    if (body != null && body.path("code").asInt(-1) == status
                            && body.path("message").isTextual()) {
                        String message = body.path("message").asText();
                        if (!message.isBlank() && !"内部管理调用未授权".equals(message))
                            throw new AdminException(status, message);
                    }
                } catch (java.io.IOException ignored) {
                    // 非标准响应不透传，避免泄漏远程服务内部信息。
                }
            }
            // 不记录异常全文或请求头，避免 Token 和服务凭证进入日志。
            log.warn("管理 RPC 请求失败 status={}", status);
            throw unavailable();
        }
    }

    private AdminException unavailable() {
        return new AdminException(503, "业务服务暂不可用，请刷新数据确认结果后重试");
    }
}
