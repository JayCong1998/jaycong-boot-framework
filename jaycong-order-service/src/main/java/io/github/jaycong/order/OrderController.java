package io.github.jaycong.order;

import io.github.jaycong.core.model.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderService service;
    public OrderController(OrderService service) { this.service = service; }

    @PostMapping
    public Result<Order> create(@Valid @RequestBody CreateOrderRequest request) {
        return Result.success(service.create(request));
    }
    @GetMapping("/{id}")
    public Result<Order> get(@PathVariable @Positive Long id) { return Result.success(service.get(id)); }
    @GetMapping
    public Result<List<Order>> list() { return Result.success(service.list()); }
}
