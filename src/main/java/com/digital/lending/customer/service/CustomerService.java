package com.digital.lending.customer.service;

import com.digital.lending.customer.dto.CustomerDto;
import com.digital.lending.customer.dto.CreateCustomerRequest;
import com.digital.lending.customer.dto.UpdateCustomerRequest;

import java.util.List;
import java.util.Optional;

public interface CustomerService {
    CustomerDto createCustomer(CreateCustomerRequest request);
    Optional<CustomerDto> findCustomerById(String id);
    List<CustomerDto> findAllCustomers();
    CustomerDto updateCustomer(String id, UpdateCustomerRequest request);
    void deleteCustomer(String id);
    boolean isCustomerActive(String id);
}