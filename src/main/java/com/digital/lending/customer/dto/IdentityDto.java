package com.digital.lending.customer.dto;

import com.digital.lending.customer.enums.DocumentType;

public record IdentityDto(DocumentType documentType, String documentNumber) {}