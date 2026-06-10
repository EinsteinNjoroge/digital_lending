package com.digital.lending.profile;

import com.digital.lending.profile.controller.ProfileController;
import com.digital.lending.profile.dto.CreateProfileRequestDto;
import com.digital.lending.profile.service.ProfileService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ProfileController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        }
)
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProfileService profileService;

    private ResultActions postProfile(String json) throws Exception {
        return mockMvc.perform(post("/api/v1/profiles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private static String validIndividualJson() {
        return """
                {
                    "profileType": "INDIVIDUAL",
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
                    "profileType": "CORPORATE",
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
            postProfile(validIndividualJson())
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Should pass request through when processing a valid corporate payload")
        void shouldProcessValidCorporate() throws Exception {
            postProfile(validCorporateJson())
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("Input Validation & Format Constraint Failures")
    class ValidationFailureTests {

        @Test
        @DisplayName("Should fail validation when individual profile has no identities")
        void shouldFailWhenIndividualHasNoIdentities() throws Exception {
            CreateProfileRequestDto.Individual invalidRequest =
                    new CreateProfileRequestDto.Individual(
                            "test@lending.global",
                            "+254",
                            "712345678",
                            "KEN",
                            "Alex",
                            "Njoroge",
                            List.of(),
                            LocalDate.of(1996, 6, 7)
                    );

            postProfile(objectMapper.writeValueAsString(invalidRequest))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
        }

        @Test
        @DisplayName("Should reject request with HTTP 400 when an invalid string is passed for DocumentType enum")
        void shouldRejectInvalidDocumentTypeEnum() throws Exception {
            String malformedJson = """
                    {
                        "profileType": "INDIVIDUAL",
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

            postProfile(malformedJson)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT_FORMAT"));
        }

        @Test
        @DisplayName("Should reject request when polymorphic discriminator profileType is missing")
        void shouldRejectMissingProfileType() throws Exception {
            String missingDiscriminatorJson = """
                    {
                        "email": "no-type@lending.global",
                        "phoneCountryCode": "+254",
                        "phoneNationalNumber": "712345678",
                        "residenceCountry": "KEN"
                    }
                    """;

            postProfile(missingDiscriminatorJson)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT_FORMAT"));
        }
    }
}
