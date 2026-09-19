package io.github.jaycong.stock;

import io.github.jaycong.core.model.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stocks")
public class StockController {
    private final StockService service;
    public StockController(StockService service) { this.service = service; }

    @GetMapping("/{productId}")
    public Result<Stock> get(@PathVariable @Positive Long productId) {
        return Result.success(service.get(productId));
    }

    @PostMapping("/deduct")
    public Result<Void> deduct(@Valid @RequestBody DeductRequest request) {
        service.deduct(request);
        return Result.success();
    }
}
