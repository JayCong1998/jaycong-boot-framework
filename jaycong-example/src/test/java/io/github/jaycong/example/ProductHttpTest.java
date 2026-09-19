package io.github.jaycong.example;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductHttpTest {
    @Autowired TestRestTemplate client;

    @Test void crudAndPaginationUseDatabase() {
        var created = client.postForEntity("/example/products", json("{\"name\":\"Book\"}"), JsonNode.class);
        assertThat(created.getStatusCode().value()).isEqualTo(200);
        long id = created.getBody().path("data").path("id").asLong();
        assertThat(id).isPositive();
        assertThat(created.getBody().path("data").path("createdAt").asText()).isNotBlank();
        var found = client.getForObject("/example/products/" + id, JsonNode.class);
        assertThat(found.path("data").path("name").asText()).isEqualTo("Book");
        var page = client.getForObject("/example/products?page=1&size=1", JsonNode.class);
        assertThat(page.path("data").path("total").asLong()).isPositive();
        assertThat(page.path("data").path("rows").size()).isEqualTo(1);
        var updated = client.exchange("/example/products/" + id, HttpMethod.PUT,
                json("{\"name\":\"Notebook\"}"), JsonNode.class);
        assertThat(updated.getStatusCode().value()).isEqualTo(200);
        assertThat(client.getForObject("/example/products/" + id, JsonNode.class)
                .path("data").path("name").asText()).isEqualTo("Notebook");
        assertThat(client.exchange("/example/products/" + id, HttpMethod.DELETE, null, JsonNode.class)
                .getStatusCode().value()).isEqualTo(200);
        assertThat(client.getForEntity("/example/products/" + id, JsonNode.class)
                .getStatusCode().value()).isEqualTo(404);
        assertThat(client.exchange("/example/products/" + id, HttpMethod.PUT,
                json("{\"name\":\"Gone\"}"), JsonNode.class).getStatusCode().value()).isEqualTo(404);
        assertThat(client.exchange("/example/products/" + id, HttpMethod.DELETE, null, JsonNode.class)
                .getStatusCode().value()).isEqualTo(404);
    }

    @Test void rejectsInvalidInput() {
        assertThat(client.postForEntity("/example/products", json("{\"name\":\" \"}"), JsonNode.class)
                .getStatusCode().value()).isEqualTo(400);
        for (String query : new String[]{"page=0", "size=0", "size=1001", "page=abc"}) {
            var response = client.getForEntity("/example/products?" + query, JsonNode.class);
            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody().path("code").asInt()).isEqualTo(400);
        }
    }

    private HttpEntity<String> json(String body) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
