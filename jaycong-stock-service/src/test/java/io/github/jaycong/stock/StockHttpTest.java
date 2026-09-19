package io.github.jaycong.stock;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = StockApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StockHttpTest {
    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach void reset() {
        jdbc.update("DELETE FROM demo_stock");
        jdbc.update("INSERT INTO demo_stock VALUES (1, 100)");
    }

    @Test void deductsAndQueriesStock() {
        assertThat(http.getForEntity("/stocks/1", JsonNode.class).getStatusCode().value()).isEqualTo(200);
        assertThat(deduct(1, 2)).isEqualTo(200);
        assertThat(http.getForObject("/stocks/1", JsonNode.class).path("data")
                .path("availableQuantity").asInt()).isEqualTo(98);
        assertThat(http.getForEntity("/stocks/999", JsonNode.class).getStatusCode().value()).isEqualTo(404);
    }

    @Test void rejectsInsufficientAndMissingStock() {
        assertThat(deduct(1, 101)).isEqualTo(422);
        assertThat(deduct(999, 1)).isEqualTo(404);
        assertThat(remaining()).isEqualTo(100);
    }

    @Test void rejectsInvalidInputWithoutChangingStock() {
        assertThat(deduct(1, 0)).isEqualTo(400);
        assertThat(deduct(1, -1)).isEqualTo(400);
        assertThat(deduct(0, 1)).isEqualTo(400);
        assertThat(http.postForEntity("/stocks/deduct", Map.of("productId", 1), JsonNode.class)
                .getStatusCode().value()).isEqualTo(400);
        assertThat(remaining()).isEqualTo(100);
    }

    @Test void concurrentDeductionsCannotOversell() throws Exception {
        jdbc.update("UPDATE demo_stock SET available_quantity = 5 WHERE product_id = 1");
        try (var pool = Executors.newFixedThreadPool(8)) {
            var tasks = IntStream.range(0, 20).mapToObj(i -> (Callable<Integer>) () -> deduct(1, 1)).toList();
            var results = pool.invokeAll(tasks);
            int successes = 0;
            for (var result : results) {
                int status = result.get();
                assertThat(status).isIn(200, 422);
                if (status == 200) successes++;
            }
            assertThat(successes).isEqualTo(5);
        }
        assertThat(remaining()).isZero();
    }

    private int deduct(long productId, int quantity) {
        return http.postForEntity("/stocks/deduct", Map.of("productId", productId, "quantity", quantity),
                JsonNode.class).getStatusCode().value();
    }
    private int remaining() {
        return jdbc.queryForObject("SELECT available_quantity FROM demo_stock WHERE product_id = 1", Integer.class);
    }
}
