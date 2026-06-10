package com.digital.lending.payment.controller;

import com.digital.lending.payment.dto.PaymentExecutionRequestDto;
import com.digital.lending.payment.dto.PaymentProviderCallbackRequestDto;
import com.digital.lending.payment.dto.PaymentResponseDto;
import com.digital.lending.payment.service.PaymentProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Management", description = "Decoupled transaction execution ledgers across heterogeneous provider networks")
public class PaymentController {

    private final PaymentProcessingService service;

    @PostMapping
    @Operation(summary = "Process financial payments", description = "Executes direct payments through the synchronous compatibility path.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Payment successfully recorded, ledger updated, and event dispatched",
                    content = @Content(schema = @Schema(implementation = PaymentResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid transactional parameters or data schema validation breach"),
            @ApiResponse(responseCode = "409", description = "Idempotency key violation conflict detected")
    })
    public ResponseEntity<PaymentResponseDto> executeTransaction(@Valid @RequestBody PaymentExecutionRequestDto request) {
        PaymentResponseDto response = service.registerAndProcessPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/providers/{providerId}/callback")
    @Operation(summary = "Process provider callback", description = "Finalizes asynchronous disbursement or repayment processing using the provider callback payload.")
    public ResponseEntity<PaymentResponseDto> processProviderCallback(
            @PathVariable String providerId,
            @Valid @RequestBody PaymentProviderCallbackRequestDto request) {
        return ResponseEntity.ok(service.processProviderCallback(providerId, request));
    }

    @GetMapping
    @Operation(summary = "List all payments with filters", description = "Retrieves a paginated list of transaction records filtered by dates, reference tracking lines, or network platforms.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paginated ledger snapshot returned successfully")
    })
    public ResponseEntity<Page<PaymentResponseDto>> getPayments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) String profileId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) String accountReference,
            @RequestParam(required = false) String providerId,
            @RequestParam(required = false) String currency,
            @PageableDefault(size = 20, sort = "initiatedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<PaymentResponseDto> payments = service.getFilteredPayments(
                fromDate, toDate, profileId, accountReference, providerId, currency, pageable
        );
        return ResponseEntity.ok(payments);
    }
}
