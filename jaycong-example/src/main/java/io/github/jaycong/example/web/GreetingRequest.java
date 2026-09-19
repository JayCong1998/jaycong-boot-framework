package io.github.jaycong.example.web;

import jakarta.validation.constraints.NotBlank;

public record GreetingRequest(@NotBlank(message = "名称不能为空") String name) { }
