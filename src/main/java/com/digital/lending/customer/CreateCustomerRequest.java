package com.digital.lending.customer;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "customerType"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = CreateCustomerRequest.Individual.class, name = "INDIVIDUAL"),
    @JsonSubTypes.Type(value = CreateCustomerRequest.Corporate.class, name = "CORPORATE"),
    @JsonSubTypes.Type(value = CreateCustomerRequest.Joint.class, name = "JOINT")
})
public interface CreateCustomerRequest {
    String email();
    String phoneCountryCode();
    String phoneNationalNumber();
    String residenceCountry();

    record IdentityInput(
        @NotNull(message = "Document type is required") DocumentType documentType,
        @NotEmpty(message = "Document number cannot be empty") String documentNumber
    ) {}

    record Individual(
        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        String email,
        String phoneCountryCode,
        String phoneNationalNumber,
        String residenceCountry,
        String firstName,
        String lastName,
        @Valid @NotEmpty(message = "At least one identity document is required") List<IdentityInput> identities,
        LocalDate dateOfBirth
    ) implements CreateCustomerRequest {}

    record Corporate(
        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        String email,
        String phoneCountryCode,
        String phoneNationalNumber,
        String residenceCountry,
        String companyName,
        String registrationNumber,
        LocalDate incorporationDate,
        String authorizedSignatoryName,
        @Valid @NotEmpty(message = "Director identity documents are required") List<IdentityInput> directorIdentities
    ) implements CreateCustomerRequest {}

    record Joint(
        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        String email,
        String phoneCountryCode,
        String phoneNationalNumber,
        String residenceCountry,
        String accountName,
        String primaryContactName,
        @NotNull(message = "Number of applicants is required") Integer numberOfApplicants,
        @Valid @NotEmpty(message = "Applicant identity documents are required") List<IdentityInput> applicantIdentities
    ) implements CreateCustomerRequest {}
}