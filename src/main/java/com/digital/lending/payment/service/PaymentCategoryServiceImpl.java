package com.digital.lending.payment.service;

import com.digital.lending.payment.dto.PaymentCategoryRequest;
import com.digital.lending.payment.dto.PaymentCategoryResponse;
import com.digital.lending.payment.model.PaymentCategory;
import com.digital.lending.payment.repository.PaymentCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentCategoryServiceImpl implements PaymentCategoryService {

    private final PaymentCategoryRepository repository;

    @Override
    public PaymentCategoryResponse create(PaymentCategoryRequest request) {

        PaymentCategory entity = PaymentCategory.builder()
                .id(request.id())
                .name(request.name())
                .description(request.description())
                .createdAt(Instant.now())
                .build();

        repository.save(entity);

        return toResponse(entity);
    }

    @Override
    public PaymentCategoryResponse get(String id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Category not found"));
    }

    @Override
    public List<PaymentCategoryResponse> getAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public PaymentCategoryResponse update(String id, PaymentCategoryRequest request) {
        PaymentCategory entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        entity.setName(request.name());
        entity.setDescription(request.description());

        return toResponse(repository.save(entity));
    }

    @Override
    public void delete(String id) {
        repository.deleteById(id);
    }

    private PaymentCategoryResponse toResponse(PaymentCategory e) {
        return new PaymentCategoryResponse(
                e.getId(),
                e.getName(),
                e.getDescription(),
                e.getCreatedAt()
        );
    }
}
