package com.digital.lending.profile.dto;

import com.digital.lending.profile.enums.DocumentType;

public record IdentityDto(DocumentType documentType, String documentNumber) {}
