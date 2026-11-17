


package io.github.jaycong.web.access;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
public class RequestLogFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID = "requestId";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // 1. 生成请求唯一标识
        String requestId = UUID.randomUUID().toString()
                .replace("-", "");

        // 2. 放入 MDC，方便其他业务日志获取
        String previousRequestId = MDC.get(REQUEST_ID);
        MDC.put(REQUEST_ID, requestId);

        // 3. 将 RequestId 返回给客户端
        response.setHeader(REQUEST_ID_HEADER, requestId);

        long startTime = System.nanoTime();

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String clientIp = request.getRemoteAddr();

        String exception = null;

        try {
            log.info("HTTP请求开始 requestId={} method={} uri={} ip={}",
                    requestId, method, uri, clientIp);
            // 4. 执行业务请求
            filterChain.doFilter(request, response);

        } catch (IOException | ServletException | RuntimeException e) {
            exception = e.getClass().getSimpleName();
            throw e;

        } finally {
            // 5. 计算请求耗时
            long cost = (System.nanoTime() - startTime) / 1_000_000;

            int status = response.getStatus();

            // 6. 统一打印请求结束日志
            if (exception != null || status >= 500) {
                log.error(
                        "HTTP请求结束 requestId={} method={} uri={} ip={} status={} cost={}ms exception={}",
                        requestId, method, uri, clientIp,
                        status, cost, exception
                );
            } else if (status >= 400 || cost >= 3000) {
                log.warn(
                        "HTTP请求结束 requestId={} method={} uri={} ip={} status={} cost={}ms",
                        requestId, method, uri, clientIp,
                        status, cost
                );
            } else {
                log.info(
                        "HTTP请求结束 requestId={} method={} uri={} ip={} status={} cost={}ms",
                        requestId, method, uri, clientIp,
                        status, cost
                );
            }

            // 7. 清理 MDC，防止线程复用造成日志串号
            if (previousRequestId == null) {
                MDC.remove(REQUEST_ID);
            } else {
                MDC.put(REQUEST_ID, previousRequestId);
            }
        }
    }
}
