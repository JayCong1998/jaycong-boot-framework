package io.github.jaycong.order;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.jaycong.stock.StockApplication;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

class OrderFlowTest {
    static ServletWebServerApplicationContext stock;
    static ServletWebServerApplicationContext order;
    static final TestRestTemplate http = new TestRestTemplate();
    static String orderUrl;
    static String stockUrl;

    @BeforeAll static void startServices() {
        stock = (ServletWebServerApplicationContext) new SpringApplicationBuilder(StockApplication.class)
                .run("--server.port=0", "--spring.datasource.generate-unique-name=true",
                        "--spring.sql.init.schema-locations=classpath:db/stock-h2.sql");
        stockUrl = "http://localhost:" + stock.getWebServer().getPort();
        try {
            order = (ServletWebServerApplicationContext) new SpringApplicationBuilder(OrderApplication.class)
                    .run("--server.port=0", "--spring.datasource.generate-unique-name=true",
                            "--spring.sql.init.schema-locations=classpath:db/order-h2.sql",
                            "--stock.base-url=" + stockUrl);
            orderUrl = "http://localhost:" + order.getWebServer().getPort();
        } catch (RuntimeException e) {
            stock.close();
            throw e;
        }
    }

    @AfterAll static void stopServices() {
        if (order != null) order.close();
        if (stock != null) stock.close();
    }

    @BeforeEach void reset() {
        order.getBean(JdbcTemplate.class).update("DELETE FROM demo_order");
        var db = stock.getBean(JdbcTemplate.class);
        db.update("DELETE FROM demo_stock");
        db.update("INSERT INTO demo_stock VALUES (1, 100)");
    }

    @Test void successfulOrderCommitsBothDatabases() {
        var response = http.postForEntity(orderUrl + "/orders",
                Map.of("productId", 1, "quantity", 2), JsonNode.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        var data = response.getBody().path("data");
        assertThat(data.path("id").asLong()).isPositive();
        assertThat(data.path("productId").asLong()).isEqualTo(1);
        assertThat(data.path("quantity").asInt()).isEqualTo(2);
        var found = http.getForObject(orderUrl + "/orders/" + data.path("id").asLong(), JsonNode.class);
        assertThat(found.path("data")).isEqualTo(data);
        assertThat(orderCount()).isEqualTo(1);
        assertThat(stockRemaining()).isEqualTo(98);
    }

    @Test void failureAfterRemoteCommitRollsBackOnlyOrder() {
        var response = http.postForEntity(orderUrl + "/orders",
                Map.of("productId", 1, "quantity", 2, "failAfterStockDeduction", true), JsonNode.class);
        assertThat(response.getStatusCode().value()).isEqualTo(422);
        assertThat(response.getBody().path("code").asInt()).isEqualTo(21002);
        assertThat(orderCount()).isZero();
        assertThat(stockRemaining()).isEqualTo(98);
    }

    @Test void insufficientStockRollsBackOrder() {
        var response = http.postForEntity(orderUrl + "/orders",
                Map.of("productId", 1, "quantity", 101), JsonNode.class);
        assertThat(response.getStatusCode().value()).isEqualTo(422);
        assertThat(response.getBody().path("code").asInt()).isEqualTo(21001);
        assertThat(orderCount()).isZero();
        assertThat(stockRemaining()).isEqualTo(100);
    }

    @Test void missingProductRollsBackOrder() {
        assertThat(http.postForEntity(orderUrl + "/orders",
                Map.of("productId", 999, "quantity", 1), JsonNode.class).getStatusCode().value()).isEqualTo(422);
        assertThat(orderCount()).isZero();
        assertThat(stockRemaining()).isEqualTo(100);
    }

    @Test void invalidRequestsDoNotWriteEitherDatabase() {
        for (var body : java.util.List.of(Map.of("productId", 1, "quantity", 0),
                Map.of("productId", 0, "quantity", 1), Map.of("quantity", 1),
                Map.of("productId", 1))) {
            assertThat(http.postForEntity(orderUrl + "/orders", body, JsonNode.class)
                    .getStatusCode().value()).isEqualTo(400);
        }
        assertThat(orderCount()).isZero();
        assertThat(stockRemaining()).isEqualTo(100);
        assertThat(http.getForEntity(orderUrl + "/orders/999", JsonNode.class)
                .getStatusCode().value()).isEqualTo(404);
    }

    private int orderCount() {
        var response = http.getForEntity(orderUrl + "/orders", JsonNode.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        int count = response.getBody().path("data").size();
        assertThat(order.getBean(JdbcTemplate.class).queryForObject("SELECT COUNT(*) FROM demo_order", Integer.class))
                .isEqualTo(count);
        return count;
    }
    private int stockRemaining() {
        return http.getForObject(stockUrl + "/stocks/1", JsonNode.class).path("data").path("availableQuantity").asInt();
    }
}
