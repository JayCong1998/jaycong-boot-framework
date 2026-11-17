package io.github.jaycong.gateway.security;

import cn.dev33.satoken.stp.StpLogic;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/** 在网关统一验证登录会话；角色和资源级授权仍由后端业务服务负责。 */
@Component
public class GatewayAuthenticationFilter implements GlobalFilter, Ordered {
    private static final String TOKEN_PREFIX = "Bearer ";

    private final StpLogic sessions;
    private final ObjectMapper json;

    public GatewayAuthenticationFilter(StpLogic sessions, ObjectMapper json) {
        this.sessions = sessions;
        this.json = json;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (!requiresLogin(request.getMethod().name(), request.getPath().value())) {
            return chain.filter(exchange);
        }

        String token = bearerToken(request.getHeaders());
        if (token == null || sessions.getLoginIdByToken(token) == null) {
            return unauthorized(exchange);
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private boolean requiresLogin(String method, String path) {
        if (path.equals("/admin") || path.startsWith("/admin/")
                || path.equals("/orders") || path.startsWith("/orders/")) {
            return true;
        }
        if (path.equals("/auth/me") || path.equals("/auth/logout") || path.equals("/auth/session")) {
            return true;
        }
        return path.startsWith("/auth/") && !(method.equals("POST") &&
                (path.equals("/auth/login") || path.equals("/auth/register")))
                && !path.equals("/auth/status");
    }

    private String bearerToken(HttpHeaders headers) {
        String authorization = headers.getFirst("Authorization");
        if (authorization == null || !authorization.regionMatches(true, 0, TOKEN_PREFIX, 0, TOKEN_PREFIX.length())) {
            return null;
        }
        String token = authorization.substring(TOKEN_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body;
        try {
            body = json.writeValueAsBytes(Map.of("code", 401, "message", "未登录或登录已过期", "data", ""));
        } catch (Exception exception) {
            body = "{\"code\":401,\"message\":\"未登录或登录已过期\",\"data\":\"\"}"
                    .getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }
}
