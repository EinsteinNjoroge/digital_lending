package com.digital.lending.customer;

import com.digital.lending.customer.dto.CreateCustomerRequest;
import com.digital.lending.customer.dto.CustomerDto;
import com.digital.lending.customer.enums.CustomerStatus;
import com.digital.lending.customer.enums.DocumentType;
import com.digital.lending.customer.exception.CustomerDomainException;
import com.digital.lending.customer.exception.DuplicateIdentityException;
import com.digital.lending.customer.model.CorporateCustomer;
import com.digital.lending.customer.model.IdentityDocument;
import com.digital.lending.customer.model.IndividualCustomer;
import com.digital.lending.customer.repository.CustomerRepository;
import com.digital.lending.customer.service.impl.CustomerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class CustomerServiceTest {

    @Autowired
    private CustomerRepository repository;

    private CustomerServiceImpl customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerServiceImpl(repository);
    }

    @Nested
    @DisplayName("Individual Profile Scenarios")
    class IndividualProfileTests {

        @Test
        @DisplayName("Should onboard valid individual profile")
        void shouldOnboardValidIndividualProfile() {
            CreateCustomerRequest.Individual validRequest = individualRequest(
                    "alex.njoroge@lending.global",
                    "Alex",
                    "Njoroge",
                    "12345678"
            );

            CustomerDto result = customerService.createCustomer(validRequest);

            assertNotNull(result.id());
            assertEquals("INDIVIDUAL", result.customerType());
            assertEquals("ACTIVE", result.status());
            assertEquals(1, result.identities().size());
        }

        @Test
        @DisplayName("Should fail when individual identity is duplicated")
        void shouldFailWhenIndividualIdentityDuplicated() {
            IndividualCustomer seed = individualSeed("88888888");
            repository.saveAndFlush(seed);

            CreateCustomerRequest.Individual duplicateRequest = individualRequest(
                    "second@lending.global",
                    "Second",
                    "User",
                    "88888888"
            );

            assertThrows(DuplicateIdentityException.class, () -> {
                customerService.createCustomer(duplicateRequest);
                repository.flush();
            });
        }
    }

    @Nested
    @DisplayName("Corporate Profile Scenarios")
    class CorporateProfileTests {

        @Test
        @DisplayName("Should onboard valid corporate profile")
        void shouldOnboardValidCorporateProfile() {
            CreateCustomerRequest.Corporate validRequest = corporateRequest("CPR-2026-XYZ89");

            CustomerDto result = customerService.createCustomer(validRequest);

            assertNotNull(result.id());
            assertEquals("CORPORATE", result.customerType());
            assertEquals("ACTIVE", result.status());
        }

        @Test
        @DisplayName("Should fail when corporate registration number exists")
        void shouldFailWhenCorporateRegistrationNumberExists() {
            CorporateCustomer seed = corporateSeed("CPR-2026-UNIQUE");
            repository.saveAndFlush(seed);

            CreateCustomerRequest.Corporate duplicateRequest = corporateRequest("CPR-2026-UNIQUE");

            assertThrows(DataIntegrityViolationException.class, () -> {
                customerService.createCustomer(duplicateRequest);
                repository.flush();
            });
        }
    }

    @Nested
    @DisplayName("Joint Profile Scenarios")
    class JointProfileTests {

        @Test
        @DisplayName("Should fail when joint identity count mismatches applicant count")
        void shouldThrowExceptionOnJointIdentityCountMismatch() {
            CreateCustomerRequest.Joint request = new CreateCustomerRequest.Joint(
                    "joint@lending.global",
                    "+254",
                    "711222333",
                    "KEN",
                    "Alex & Mary Joint Account",
                    "Alex Njoroge",
                    3,
                    List.of(new CreateCustomerRequest.IdentityInput(DocumentType.NATIONAL_ID, "112233"))
            );

            CustomerDomainException ex = assertThrows(CustomerDomainException.class, () -> {
                customerService.createCustomer(request);
                repository.flush();
            });

            assertEquals("JOINT_IDENTITIES_MISMATCH", ex.getErrorCode());
        }

        @Test
        @DisplayName("Should fail when identity already exists globally")
        void shouldEnforceGlobalIdentityUniqueness() {
            repository.saveAndFlush(individualSeed("555666"));

            CreateCustomerRequest.Joint request = new CreateCustomerRequest.Joint(
                    "newjoint@lending.global",
                    "+254",
                    "733444555",
                    "KEN",
                    "Duplicate Test Account",
                    "Mary Njoki",
                    1,
                    List.of(new CreateCustomerRequest.IdentityInput(DocumentType.NATIONAL_ID, "555666"))
            );

            assertThrows(DuplicateIdentityException.class, () -> {
                customerService.createCustomer(request);
                repository.flush();
            });
        }

        @Test
        @DisplayName("Should onboard valid joint profile")
        void shouldOnboardValidJointProfile() {
            CreateCustomerRequest.Joint request = new CreateCustomerRequest.Joint(
                    "success.joint@lending.global",
                    "+254",
                    "755666777",
                    "KEN",
                    "Cohesive Team",
                    "Lead Contact",
                    2,
                    List.of(
                            new CreateCustomerRequest.IdentityInput(DocumentType.NATIONAL_ID, "999111"),
                            new CreateCustomerRequest.IdentityInput(DocumentType.PASSPORT, "999222")
                    )
            );

            CustomerDto result = customerService.createCustomer(request);

            assertNotNull(result.id());
            assertEquals("JOINT", result.customerType());
            assertEquals(2, result.identities().size());
            assertEquals("ACTIVE", result.status());
        }
    }

    @Nested
    @DisplayName("Global Integrity Scenarios")
    class IntegrityBoundaryTests {

        @Test
        @DisplayName("Should fail when email already exists")
        void shouldRejectDuplicateEmailsGlobally() {
            repository.saveAndFlush(individualSeed("clash@lending.global", "AK009911"));

            CreateCustomerRequest.Individual request = individualRequest(
                    "clash@lending.global",
                    "Distinct",
                    "Name",
                    "AK009922"
            );

            assertThrows(DuplicateIdentityException.class, () -> { // Changed to DuplicateIdentityException
                customerService.createCustomer(request);
                repository.flush();
            });
        }
    }

    private CreateCustomerRequest.Individual individualRequest(String email, String first, String last, String idNumber) {
        return new CreateCustomerRequest.Individual(
                email,
                "+254",
                "712345678",
                "KEN",
                first,
                last,
                List.of(new CreateCustomerRequest.IdentityInput(DocumentType.NATIONAL_ID, idNumber)),
                LocalDate.of(1996, 6, 7)
        );
    }

    private IndividualCustomer individualSeed(String idNumber) {
        return individualSeed("first@lending.global", idNumber);
    }

    private IndividualCustomer individualSeed(String email, String idNumber) {
        IndividualCustomer c = new IndividualCustomer();
        c.setId("SEED-IND-999");
        c.setEmail(email);
        c.setPhoneCountryCode("+254");
        c.setPhoneNationalNumber("700000000");
        c.setResidenceCountry("KEN");
        c.setStatus(CustomerStatus.ACTIVE);
        c.setFirstName("First");
        c.setLastName("Last");
        c.setDateOfBirth(LocalDate.of(1990, 1, 1));
        c.getIdentities().add(new IdentityDocument(DocumentType.NATIONAL_ID, idNumber));
        return c;
    }

    private CreateCustomerRequest.Corporate corporateRequest(String regNo) {
        return new CreateCustomerRequest.Corporate(
                "finance@devcorp.co.ke",
                "+254",
                "700111222",
                "KEN",
                "DevCorp Solutions Ltd",
                regNo,
                LocalDate.of(2020, 1, 15),
                "John Doe",
                List.of(new CreateCustomerRequest.IdentityInput(DocumentType.NATIONAL_ID, "87654321"))
        );
    }

    private CorporateCustomer corporateSeed(String regNo) {
        CorporateCustomer c = new CorporateCustomer();
        c.setId("SEED-CORP-888");
        c.setEmail("hq@devcorp.co.ke");
        c.setPhoneCountryCode("+254");
        c.setPhoneNationalNumber("700111222");
        c.setResidenceCountry("KEN");
        c.setStatus(CustomerStatus.ACTIVE);
        c.setCompanyName("DevCorp Solutions Ltd");
        c.setRegistrationNumber(regNo);
        c.setIncorporationDate(LocalDate.of(2020, 1, 15));
        c.setAuthorizedSignatoryName("John Doe");
        return c;
    }
}