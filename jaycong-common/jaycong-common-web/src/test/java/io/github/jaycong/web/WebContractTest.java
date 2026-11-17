package io.github.jaycong.web;

import io.github.jaycong.core.error.CommonErrorCode;
import io.github.jaycong.core.exception.BusinessException;
import io.github.jaycong.core.model.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = WebContractTest.Application.class)
@AutoConfigureMockMvc
class WebContractTest {
    @Autowired MockMvc mvc;

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import(Endpoints.class)
    static class Application { }

    @RestController
    static class Endpoints {
        @PostMapping("/greet")
        Result<String> greet(@Valid @RequestBody Input input) { return Result.success(input.name()); }
        @GetMapping("/number")
        Result<Integer> number(@RequestParam @Min(1) int value) { return Result.success(value); }
        @GetMapping("/business")
        void business() { throw new BusinessException(CommonErrorCode.NOT_FOUND, "目标不存在"); }
        @GetMapping("/unexpected")
        void unexpected() { throw new IllegalStateException("private-database-password"); }
    }
    record Input(@NotBlank(message = "名称不能为空") String name) { }

    @Test void successfulRequestPreservesResult() throws Exception {
        mvc.perform(post("/greet").contentType("application/json").content("{\"name\":\"Jay\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.code").isNumber()).andExpect(jsonPath("$.data").value("Jay"));
    }
    @Test void invalidBodyReturnsUnifiedError() throws Exception {
        mvc.perform(post("/greet").contentType("application/json").content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false)).andExpect(jsonPath("$.code").value(400));
    }
    @Test void malformedJsonReturnsUnifiedError() throws Exception {
        mvc.perform(post("/greet").contentType("application/json").content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false)).andExpect(jsonPath("$.code").value(400));
    }
    @Test void methodValidationReturnsUnifiedError() throws Exception {
        mvc.perform(get("/number").param("value", "0"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false)).andExpect(jsonPath("$.code").value(400));
    }
    @Test void missingParameterReturnsUnifiedError() throws Exception {
        mvc.perform(get("/number")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false)).andExpect(jsonPath("$.code").value(400));
    }
    @Test void typeMismatchReturnsUnifiedError() throws Exception {
        mvc.perform(get("/number").param("value", "abc")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false)).andExpect(jsonPath("$.code").value(400));
    }
    @Test void businessErrorPreservesPublicCodeAndMessage() throws Exception {
        mvc.perform(get("/business")).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false)).andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("目标不存在"))
                .andExpect(jsonPath("$.data").isEmpty());
    }
    @Test void unexpectedErrorDoesNotLeakDetails() throws Exception {
        mvc.perform(get("/unexpected")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false)).andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("系统异常"));
    }
    @Test void unsupportedMethodKeepsHttpStatus() throws Exception {
        mvc.perform(get("/greet")).andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(405));
    }
    @Test void missingRouteReturnsNotFound() throws Exception {
        mvc.perform(get("/missing")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false)).andExpect(jsonPath("$.code").value(404));
    }
}
