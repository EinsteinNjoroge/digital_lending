package com.digital.lending.payment.controller;

import com.digital.lending.payment.dto.PaymentCategoryRequest;
import com.digital.lending.payment.dto.PaymentCategoryResponse;
import com.digital.lending.payment.service.PaymentCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payment/categories")
@RequiredArgsConstructor
@Tag(name = "Payment Management")
public class PaymentCategoryController {

    private final PaymentCategoryService service;

    @PostMapping
    @Operation(summary = "Create category")
    public PaymentCategoryResponse create(@Valid @RequestBody PaymentCategoryRequest request) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    public PaymentCategoryResponse get(@PathVariable String id) {
        return service.get(id);
    }

    @GetMapping
    public List<PaymentCategoryResponse> getAll() {
        return service.getAll();
    }

    @PutMapping("/{id}")
    public PaymentCategoryResponse update(
            @PathVariable String id,
            @Valid @RequestBody PaymentCategoryRequest request
    ) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
