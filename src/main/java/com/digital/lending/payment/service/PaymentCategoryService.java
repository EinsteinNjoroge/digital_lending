package com.digital.lending.payment.service;

import com.digital.lending.payment.dto.PaymentCategoryRequest;
import com.digital.lending.payment.dto.PaymentCategoryResponse;

import java.util.List;

public interface PaymentCategoryService {

    PaymentCategoryResponse create(PaymentCategoryRequest request);

    PaymentCategoryResponse get(String id);

    List<PaymentCategoryResponse> getAll();

    PaymentCategoryResponse update(String id, PaymentCategoryRequest request);

    void delete(String id);
}
