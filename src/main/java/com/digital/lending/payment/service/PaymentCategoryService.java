package com.digital.lending.payment.service;

import com.digital.lending.payment.dto.PaymentCategoryRequestDto;
import com.digital.lending.payment.dto.PaymentCategoryResponseDto;

import java.util.List;

public interface PaymentCategoryService {

    PaymentCategoryResponseDto create(PaymentCategoryRequestDto request);

    PaymentCategoryResponseDto get(String id);

    List<PaymentCategoryResponseDto> getAll();

    PaymentCategoryResponseDto update(String id, PaymentCategoryRequestDto request);

    void delete(String id);
}
