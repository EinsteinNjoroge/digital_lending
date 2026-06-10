package com.digital.lending.payment.service;

import com.digital.lending.payment.dto.PaymentCategoryRequestDto;
import com.digital.lending.payment.dto.PaymentCategoryResponseDto;
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
    public PaymentCategoryResponseDto create(PaymentCategoryRequestDto request) {

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
    public PaymentCategoryResponseDto get(String id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Category not found"));
    }

    @Override
    public List<PaymentCategoryResponseDto> getAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public PaymentCategoryResponseDto update(String id, PaymentCategoryRequestDto request) {
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

    private PaymentCategoryResponseDto toResponse(PaymentCategory e) {
        return new PaymentCategoryResponseDto(
                e.getId(),
                e.getName(),
                e.getDescription(),
                e.getCreatedAt()
        );
    }
}
