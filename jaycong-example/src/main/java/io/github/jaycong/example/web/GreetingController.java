package io.github.jaycong.example.web;

import io.github.jaycong.core.exception.BusinessException;
import io.github.jaycong.core.model.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/example/greetings")
public class GreetingController {
    @PostMapping
    public Result<String> greet(@Valid @RequestBody GreetingRequest request) {
        if ("admin".equalsIgnoreCase(request.name().strip())) {
            throw new BusinessException(ExampleErrorCode.NAME_RESERVED);
        }
        return Result.success("Hello, " + request.name().strip() + "!");
    }
}
