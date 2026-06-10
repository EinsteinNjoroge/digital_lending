package com.digital.lending.loanaccount.controller;

import com.digital.lending.loanaccount.dto.LoanAccountOpeningRequestDto;
import com.digital.lending.loanaccount.dto.LoanAccountResponseDto;
import com.digital.lending.loanaccount.dto.StatusModificationRequestDto;
import com.digital.lending.loanaccount.enums.PerformanceStatus;
import com.digital.lending.loanaccount.exception.ApiErrorResponse;
import com.digital.lending.loanaccount.service.LoanAccountManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/loan-accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts Management", description = "Asynchronous accounting system handling decoupled drawdown workflows.")
public class LoanAccountController {

    private final LoanAccountManagementService accountService;

    @PostMapping
    @Operation(
            summary = "Initiate an asynchronous loan drawdown request",
            description = "Creates an initial immutable record line inside the database ledger context in DRAFT status, fires an internal Spring transaction event, and immediately returns the un-underwritten asset placeholder profile frame.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Application accepted and added to processing tracking chains.",
                    content = @Content(schema = @Schema(implementation = LoanAccountResponseDto.class))),
            @ApiResponse(responseCode = "422", description = "Credit policy exception. Profile holds an unresolved overlapping exposure.",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<LoanAccountResponseDto> issueLoan(
            @Valid @RequestBody LoanAccountOpeningRequestDto request,
            @RequestHeader(value = "X-Modified-By", defaultValue = "api_gateway_user") String actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.provisionNewAccount(request, actor));
    }

    @PatchMapping("/{id}/performance-status")
    @Operation(summary = "Modify the active credit classification tracking metric for an open asset row record line")
    public ResponseEntity<LoanAccountResponseDto> patchStatus(
            @PathVariable("id") String id,
            @Valid @RequestBody StatusModificationRequestDto request,
            @RequestHeader(value = "X-Modified-By") String actor) {
        return ResponseEntity.ok(accountService.modifyPerformanceStatus(id, request, actor));
    }

    @GetMapping
    @Operation(summary = "Search ledger lines using multi-matrix slice filters")
    public ResponseEntity<Page<LoanAccountResponseDto>> getLedger(
            @RequestParam(value = "profileId", required = false) String profileId,
            @RequestParam(value = "status", required = false) PerformanceStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(accountService.fetchLedgerSlice(profileId, status, PageRequest.of(page, size)));
    }
}