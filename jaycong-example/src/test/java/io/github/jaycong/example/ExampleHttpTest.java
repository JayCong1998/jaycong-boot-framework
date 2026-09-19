package io.github.jaycong.example;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ExampleHttpTest {
    @Autowired TestRestTemplate client;

    @Test void validRequestReturnsGreeting() {
        var response = client.postForEntity("/example/greetings", json("{\"name\":\"Jay\"}"), Response.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(new Response(true, 0, "成功", "Hello, Jay!"));
    }

    @Test void blankNameIsRejectedBeforeBusinessLogic() {
        var response = client.postForEntity("/example/greetings", json("{\"name\":\" \"}"), Response.class);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo(new Response(false, 400, "参数错误", null));
    }

    @Test void reservedNameReturnsBusinessError() {
        var response = client.postForEntity("/example/greetings", json("{\"name\":\"admin\"}"), Response.class);
        assertThat(response.getStatusCode().value()).isEqualTo(422);
        assertThat(response.getBody()).isEqualTo(new Response(false, 10001, "该名称为系统保留名称", null));
    }

    private HttpEntity<String> json(String body) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    record Response(boolean success, Integer code, String message, String data) { }
}
