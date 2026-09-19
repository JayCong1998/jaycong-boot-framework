package io.github.jaycong.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

class OrderDownstreamTest {
    static HttpServer downstream;
    static ExecutorService workers;
    static ServletWebServerApplicationContext order;
    static String url;
    static volatile int status;
    static volatile String body;
    static volatile boolean delayed;
    static final TestRestTemplate http = new TestRestTemplate();

    @BeforeAll static void start() throws Exception {
        downstream = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        workers = Executors.newVirtualThreadPerTaskExecutor();
        downstream.setExecutor(workers);
        downstream.createContext("/stocks/deduct", exchange -> {
            try (exchange) {
                exchange.getRequestBody().readAllBytes();
                if (delayed) {
                    try { Thread.sleep(1200); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                }
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
                if (bytes.length > 0) exchange.getResponseBody().write(bytes);
            }
        });
        downstream.start();
        order = (ServletWebServerApplicationContext) new SpringApplicationBuilder(OrderApplication.class)
                .run("--server.port=0", "--spring.sql.init.schema-locations=classpath:db/order-h2.sql",
                        "--stock.base-url=http://localhost:" + downstream.getAddress().getPort(),
                        "--stock.connect-timeout=200ms", "--stock.read-timeout=200ms");
        url = "http://localhost:" + order.getWebServer().getPort() + "/orders";
    }

    @AfterAll static void stop() {
        if (order != null) order.close();
        if (downstream != null) downstream.stop(0);
        if (workers != null) workers.shutdownNow();
    }

    @BeforeEach void reset() {
        order.getBean(JdbcTemplate.class).update("DELETE FROM demo_order");
        delayed = false;
        status = 200;
        body = "{\"success\":true,\"code\":0,\"message\":\"成功\",\"data\":null}";
    }

    @ParameterizedTest
    @CsvSource({
        "200, EMPTY", "200, BUSINESS_ERROR", "200, MALFORMED", "200, MISSING_CODE",
        "200, WRONG_TYPE", "200, CONTRADICTORY", "503, VALID", "302, VALID"
    })
    void rejectsInvalidOrUnsuccessfulDownstreamResponse(int httpStatus, String responseType) {
        status = httpStatus;
        body = switch (responseType) {
            case "EMPTY" -> "";
            case "BUSINESS_ERROR" -> "{\"success\":false,\"code\":22001,\"message\":\"库存不足\"}";
            case "MALFORMED" -> "not-json";
            case "MISSING_CODE" -> "{\"success\":true}";
            case "WRONG_TYPE" -> "{\"success\":true,\"code\":\"0\"}";
            case "CONTRADICTORY" -> "{\"success\":false,\"code\":0}";
            default -> body;
        };
        assertRollback();
    }

    @Test void readTimeoutRollsBackOrder() {
        delayed = true;
        long started = System.nanoTime();
        assertRollback();
        assertThat((System.nanoTime() - started) / 1_000_000).isLessThan(1100);
    }

    private void assertRollback() {
        var response = http.postForEntity(url, Map.of("productId", 1, "quantity", 2), JsonNode.class);
        assertThat(response.getStatusCode().value()).isEqualTo(422);
        assertThat(response.getBody().path("code").asInt()).isEqualTo(21001);
        assertThat(order.getBean(JdbcTemplate.class).queryForObject("SELECT COUNT(*) FROM demo_order", Integer.class))
                .isZero();
    }
}
