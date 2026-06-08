package com.digital.lending.customer;

import com.digital.lending.customer.controller.CustomerController;
import com.digital.lending.customer.dto.CreateCustomerRequest;
import com.digital.lending.customer.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = CustomerController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        }
)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomerService customerService;

    private ResultActions postCustomer(String json) throws Exception {
        return mockMvc.perform(post("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private static String validIndividualJson() {
        return """
                {
                    "customerType": "INDIVIDUAL",
                    "email": "alex.njoroge@lending.global",
                    "phoneCountryCode": "+254",
                    "phoneNationalNumber": "712345678",
                    "residenceCountry": "KEN",
                    "firstName": "Alex",
                    "lastName": "Njoroge",
                    "dateOfBirth": "1996-06-07",
                    "identities": [
                        {
                            "documentType": "NATIONAL_ID",
                            "documentNumber": "12345678"
                        }
                    ]
                }
                """;
    }

    private static String validCorporateJson() {
        return """
                {
                    "customerType": "CORPORATE",
                    "email": "finance@devcorp.co.ke",
                    "phoneCountryCode": "+254",
                    "phoneNationalNumber": "700111222",
                    "residenceCountry": "KEN",
                    "companyName": "DevCorp Solutions Ltd",
                    "registrationNumber": "CPR-2026-XYZ89",
                    "incorporationDate": "2020-01-15",
                    "authorizedSignatoryName": "Jane Wangari",
                    "directorIdentities": [
                        {
                            "documentType": "NATIONAL_ID",
                            "documentNumber": "87654321"
                        }
                    ]
                }
                """;
    }

    @Nested
    @DisplayName("Happy Path: Valid inputs")
    class HappyPathTests {

        @Test
        @DisplayName("Should pass request through when processing a valid individual payload")
        void shouldProcessValidIndividual() throws Exception {
            postCustomer(validIndividualJson())
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Should pass request through when processing a valid corporate payload")
        void shouldProcessValidCorporate() throws Exception {
            postCustomer(validCorporateJson())
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("Input Validation & Format Constraint Failures")
    class ValidationFailureTests {

        @Test
        @DisplayName("Should fail validation when individual profile has no identities")
        void shouldFailWhenIndividualHasNoIdentities() throws Exception {
            CreateCustomerRequest.Individual invalidRequest =
                    new CreateCustomerRequest.Individual(
                            "test@lending.global",
                            "+254",
                            "712345678",
                            "KEN",
                            "Alex",
                            "Njoroge",
                            List.of(),
                            LocalDate.of(1996, 6, 7)
                    );

            postCustomer(objectMapper.writeValueAsString(invalidRequest))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        }

        @Test
        @DisplayName("Should reject request with HTTP 400 when an invalid string is passed for DocumentType enum")
        void shouldRejectInvalidDocumentTypeEnum() throws Exception {
            String malformedJson = """
                    {
                        "customerType": "INDIVIDUAL",
                        "email": "malformed@lending.global",
                        "phoneCountryCode": "+254",
                        "phoneNationalNumber": "712345678",
                        "residenceCountry": "KEN",
                        "firstName": "Alex",
                        "lastName": "Njoroge",
                        "dateOfBirth": "1996-06-07",
                        "identities": [
                            {
                                "documentType": "INVALID_MARKER_VALUE",
                                "documentNumber": "123456"
                            }
                        ]
                    }
                    """;

            postCustomer(malformedJson)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT_FORMAT"));
        }

        @Test
        @DisplayName("Should reject processing when incoming email layout format is corrupt")
        void shouldRejectMalformedEmailFormat() throws Exception {
            String invalidEmailJson = """
                    {
                        "customerType": "INDIVIDUAL",
                        "email": "corrupt-plain-string-without-domain",
                        "phoneCountryCode": "+254",
                        "phoneNationalNumber": "712345678",
                        "residenceCountry": "KEN",
                        "firstName": "Alex",
                        "lastName": "Njoroge",
                        "dateOfBirth": "1996-06-07",
                        "identities": [
                            {
                                "documentType": "NATIONAL_ID",
                                "documentNumber": "1234"
                            }
                        ]
                    }
                    """;

            postCustomer(invalidEmailJson)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        }

        @Test
        @DisplayName("Should reject request when polymorphic discriminator customerType is missing")
        void shouldRejectMissingCustomerType() throws Exception {
            String missingDiscriminatorJson = """
                    {
                        "email": "no-type@lending.global",
                        "phoneCountryCode": "+254",
                        "phoneNationalNumber": "712345678",
                        "residenceCountry": "KEN"
                    }
                    """;

            postCustomer(missingDiscriminatorJson)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT_FORMAT"));
        }

        @Test
        @DisplayName("Should reject request when polymorphic discriminator customerType is unsupported")
        void shouldRejectUnsupportedCustomerType() throws Exception {
            String invalidDiscriminatorJson = """
                    {
                        "customerType": "PARTNERSHIP",
                        "email": "partner@lending.global",
                        "phoneCountryCode": "+254",
                        "phoneNationalNumber": "712345678",
                        "residenceCountry": "KEN"
                    }
                    """;

            postCustomer(invalidDiscriminatorJson)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT_FORMAT"));
        }

        @Test
        @DisplayName("Should fail validation when corporate profile missing mandatory identification data")
        void shouldFailWhenCorporateMissingRequiredFields() throws Exception {
            CreateCustomerRequest.Corporate invalidCorporate =
                    new CreateCustomerRequest.Corporate(
                            "finance@devcorp.co.ke",
                            "+254",
                            "700111222",
                            "KEN",
                            "",
                            "",
                            LocalDate.of(2020, 1, 15),
                            "Jane Wangari",
                            List.of()
                    );

            postCustomer(objectMapper.writeValueAsString(invalidCorporate))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        }

        @Test
        @DisplayName("Should pass request when country formatting constraints are not strictly enforced downstream")
        void shouldPassResidenceCountry() throws Exception {
            String malformedCountryJson = """
                    {
                        "customerType": "INDIVIDUAL",
                        "email": "country@lending.global",
                        "phoneCountryCode": "+254",
                        "phoneNationalNumber": "712345678",
                        "residenceCountry": "KENYA",
                        "firstName": "Alex",
                        "lastName": "Njoroge",
                        "dateOfBirth": "1996-06-07",
                        "identities": [
                            {
                                "documentType": "NATIONAL_ID",
                                "documentNumber": "12345678"
                            }
                        ]
                    }
                    """;

            postCustomer(malformedCountryJson)
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("Data Integrity & Exception Routing Handlers")
    class IntegrityFailureTests {

        @Test
        @DisplayName("Should route clean structural code when downstream layers register duplicate keys")
        void shouldMapConflictWhenServiceThrowsDataIntegrityViolations() throws Exception {

            doThrow(new org.springframework.dao.DataIntegrityViolationException("Duplicate entity entry recorded"))
                    .when(customerService).createCustomer(any());

            postCustomer(validIndividualJson())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("DATA_INTEGRITY_CONFLICT"));
        }
    }
}