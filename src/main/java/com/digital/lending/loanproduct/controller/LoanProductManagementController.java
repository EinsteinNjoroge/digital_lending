package com.digital.lending.loanproduct.controller;

import com.digital.lending.loanproduct.dto.ProductConfigurationRequestDto;
import com.digital.lending.loanproduct.dto.ProductConfigurationResponseDto;
import com.digital.lending.loanproduct.exception.ApiErrorResponse;
import com.digital.lending.loanproduct.service.LoanProductManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@RestController
@RequestMapping("/api/v1/loan-products")
@RequiredArgsConstructor
@Tag(name = "Loan Products", description = "Loan product configuration APIs.")
public class LoanProductManagementController {

    private final LoanProductManagementService productService;

    @PostMapping
    @Operation(summary = "Create a loan product", description = "Creates a partner and currency specific product configuration.")
    @Parameter(name = "X-Modified-By", in = ParameterIn.HEADER, description = "System operator execution user token mapping coordinates", required = true, schema = @Schema(type = "string"), example = "developer.engineering@ezra.co")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Product parameters persisted safely inside ledger cache graphs.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductConfigurationResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Payload schema constraint failures or syntax compilation errors.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Business Rule Collision. Active product settings matches duplicate tracking parameters profiles.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<ProductConfigurationResponseDto> createProduct(
            @RequestHeader("X-Modified-By") String modifiedBy,
            @Valid @RequestBody ProductConfigurationRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request, modifiedBy));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a loan product", description = "Updates product metadata, parameters, and document matrices.")
    @Parameters({
            @Parameter(name = "id", in = ParameterIn.PATH, description = "The standard 36-character hyphenated UUID tracking reference string reference parameter", required = true, example = "f186e626-c8b0-4e0c-bc1a-592b68f275ca"),
            @Parameter(name = "X-Modified-By", in = ParameterIn.HEADER, description = "Operator execution entity name context parameters", required = true, example = "compliance.manager@ezra.co")
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Configuration mapping alterations applied completely.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductConfigurationResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Target instance key indicator coordinates missing inside database row allocations.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<ProductConfigurationResponseDto> updateProduct(
            @PathVariable("id") String id,
            @RequestHeader("X-Modified-By") String modifiedBy,
            @Valid @RequestBody ProductConfigurationRequestDto request) {
        return ResponseEntity.ok(productService.updateProduct(id, request, modifiedBy));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a loan product")
    public ResponseEntity<ProductConfigurationResponseDto> getProductById(@PathVariable("id") String id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping
    @Operation(summary = "List loan products")
    public ResponseEntity<List<ProductConfigurationResponseDto>> getProducts(
            @RequestParam(value = "partnerId", required = false) String partnerId,
            @RequestParam(value = "currency", required = false) String currency,
            @RequestParam(value = "isActive", required = false) Boolean isActive) {
        return ResponseEntity.ok(productService.getFilteredProducts(partnerId, currency, isActive));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a loan product")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable("id") String id,
            @RequestHeader("X-Modified-By") String modifiedBy) {
        productService.logicalDeleteProduct(id, modifiedBy);
        return ResponseEntity.noContent().build();
    }
}
