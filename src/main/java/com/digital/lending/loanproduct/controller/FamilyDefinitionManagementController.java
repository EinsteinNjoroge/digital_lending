package com.digital.lending.loanproduct.controller;

import com.digital.lending.loanproduct.dto.FamilyDefinitionRequestDto;
import com.digital.lending.loanproduct.dto.FamilyDefinitionResponseDto;
import com.digital.lending.loanproduct.exception.ApiErrorResponse;
import com.digital.lending.loanproduct.service.FamilyDefinitionManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.util.List;

@RestController
@RequestMapping("/api/v1/family-definitions")
@RequiredArgsConstructor
@Tag(name = "Loan Product Families", description = "Loan product family definition APIs.")
public class FamilyDefinitionManagementController {

    private final FamilyDefinitionManagementService familyService;

    @PostMapping
    @Operation(summary = "Create a product family", description = "Creates a reusable family definition for loan products.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Calculation framework compiled successfully and cataloged into system memory fields.", content = @Content(schema = @Schema(implementation = FamilyDefinitionResponseDto.class))),
            @ApiResponse(responseCode = "422", description = "Unprocessable Strategy Entry. A structural profile tracking equivalent system definitions tokens stands active.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<FamilyDefinitionResponseDto> createFamily(@Valid @RequestBody FamilyDefinitionRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(familyService.createFamily(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a product family")
    @Parameter(name = "id", in = ParameterIn.PATH, description = "The structural identity token key tracking the target definition template row", required = true, example = "fdef_002")
    public ResponseEntity<FamilyDefinitionResponseDto> updateFamily(
            @PathVariable("id") String id,
            @Valid @RequestBody FamilyDefinitionRequestDto request) {
        return ResponseEntity.ok(familyService.updateFamily(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a product family")
    public ResponseEntity<FamilyDefinitionResponseDto> getFamilyById(@PathVariable("id") String id) {
        return ResponseEntity.ok(familyService.getFamilyById(id));
    }

    @GetMapping
    @Operation(summary = "List product families")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registry array fetched completely.", content = @Content(array = @ArraySchema(schema = @Schema(implementation = FamilyDefinitionResponseDto.class))))
    })
    public ResponseEntity<List<FamilyDefinitionResponseDto>> getAllFamilies() {
        return ResponseEntity.ok(familyService.getAllFamilies());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a product family")
    public ResponseEntity<Void> deleteFamily(@PathVariable("id") String id) {
        familyService.logicalDeleteFamily(id);
        return ResponseEntity.noContent().build();
    }
}
