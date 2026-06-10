package com.digital.lending.payment;

import com.digital.lending.payment.dto.PaymentCategoryRequest;
import com.digital.lending.payment.dto.PaymentCategoryResponse;
import com.digital.lending.payment.model.PaymentCategory;
import com.digital.lending.payment.repository.PaymentCategoryRepository;
import com.digital.lending.payment.service.PaymentCategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentCategoryServiceTest {

    @Mock
    private PaymentCategoryRepository repository;

    @InjectMocks
    private PaymentCategoryServiceImpl service;

    private PaymentCategoryRequest request;
    private PaymentCategory entity;

    @BeforeEach
    void setUp() {
        request = new PaymentCategoryRequest("REPAYMENT", "Loan Repayment", "Repayment category");
        entity = PaymentCategory.builder()
                .id("REPAYMENT")
                .name("Loan Repayment")
                .description("Repayment category")
                .createdAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("Should create category successfully")
        void shouldCreateCategorySuccessfully() {
            when(repository.save(any(PaymentCategory.class))).thenAnswer(invocation -> invocation.getArgument(0));

            PaymentCategoryResponse response = service.create(request);

            assertEquals("REPAYMENT", response.id());
            assertEquals("Loan Repayment", response.name());
            verify(repository).save(any(PaymentCategory.class));
        }
    }

    @Nested
    @DisplayName("get")
    class GetTests {

        @Test
        @DisplayName("Should return category when found")
        void shouldReturnCategoryWhenFound() {
            when(repository.findById("REPAYMENT")).thenReturn(Optional.of(entity));

            PaymentCategoryResponse response = service.get("REPAYMENT");

            assertEquals("REPAYMENT", response.id());
            assertEquals("Loan Repayment", response.name());
        }

        @Test
        @DisplayName("Should throw runtime exception when category missing")
        void shouldThrowWhenCategoryMissing() {
            when(repository.findById("UNKNOWN")).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class, () -> service.get("UNKNOWN"));
            assertEquals("Category not found", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("getAll")
    class GetAllTests {

        @Test
        @DisplayName("Should return all categories")
        void shouldReturnAllCategories() {
            when(repository.findAll()).thenReturn(List.of(entity));

            List<PaymentCategoryResponse> result = service.getAll();

            assertEquals(1, result.size());
            assertEquals("REPAYMENT", result.getFirst().id());
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("Should update category successfully")
        void shouldUpdateCategorySuccessfully() {
            when(repository.findById("REPAYMENT")).thenReturn(Optional.of(entity));
            when(repository.save(any(PaymentCategory.class))).thenAnswer(invocation -> invocation.getArgument(0));

            PaymentCategoryResponse response = service.update(
                    "REPAYMENT",
                    new PaymentCategoryRequest("REPAYMENT", "Loan Repayment Updated", "Updated description")
            );

            assertEquals("Loan Repayment Updated", response.name());
            assertEquals("Updated description", response.description());
            verify(repository).save(entity);
        }

        @Test
        @DisplayName("Should throw when updating missing category")
        void shouldThrowWhenUpdatingMissingCategory() {
            when(repository.findById("UNKNOWN")).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> service.update("UNKNOWN", request));

            assertEquals("Category not found", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("Should delegate delete to repository")
        void shouldDeleteCategory() {
            doNothing().when(repository).deleteById("REPAYMENT");

            service.delete("REPAYMENT");

            verify(repository).deleteById("REPAYMENT");
        }
    }
}
