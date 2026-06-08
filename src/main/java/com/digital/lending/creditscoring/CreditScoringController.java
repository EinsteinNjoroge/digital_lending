package com.digital.lending.creditscoring;

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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/credit-scoring")
@RequiredArgsConstructor
@Tag(name = "Credit Scoring Domain Engine", description = "Multi-tenant rule evaluation orchestrator handling polymorphic underwriting frameworks for thin-file subscriber profiles.")
public class CreditScoringController {

    private final CreditScoringOrchestrationEngine orchestrationEngine;
    private final CreditScoringModelManagementService managementService;

    @PostMapping("/evaluate")
    @Operation(
            summary = "Evaluate customer underwriting profiles against dynamic matrix rules",
            description = "Processes incoming unstructured telemetry dataset parameters against isolated tenant-level scorecard models via an explicit real-time pipeline interpreter framework."
    )
    @Parameters({
            @Parameter(name = "X-Partner-Id", in = ParameterIn.HEADER, required = true, schema = @Schema(type = "string"), example = "SAF_KE_01"),
            @Parameter(name = "X-Currency", in = ParameterIn.HEADER, required = true, schema = @Schema(type = "string", minLength = 3, maxLength = 3), example = "KES")
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CreditDecisionResponse.class))),
            @ApiResponse(responseCode = "400", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<CreditDecisionResponse> evaluateCustomerRisk(
            @RequestHeader("X-Partner-Id") String partnerId,
            @RequestHeader("X-Currency") String currency,
            @Valid @RequestBody ScoringRequestDto request) {

        CreditDecisionResponse decision = orchestrationEngine.resolveAndEvaluate(
                partnerId,
                currency,
                request
        );

        return ResponseEntity.ok(decision);
    }

    @PostMapping("/models")
    @Operation(summary = "Provision a new multi-tenant credit scorecard rule matrix model definition record")
    public ResponseEntity<CreditScoringModelResponseDto> createScoringModel(
            @RequestHeader("X-Partner-Id") String partnerId,
            @RequestHeader("X-Currency") String currency,
            @Valid @RequestBody CreditScoringModelRequestDto request) {
        return ResponseEntity.ok(managementService.createModel(partnerId, currency, request));
    }

    @PutMapping("/models/{id}")
    @Operation(summary = "Update the live rules configuration payload payload strings mapped onto an active model instance reference")
    public ResponseEntity<CreditScoringModelResponseDto> updateScoringModel(
            @PathVariable("id") String id,
            @Valid @RequestBody CreditScoringModelRequestDto request) {
        return ResponseEntity.ok(managementService.updateModel(id, request));
    }

    @GetMapping("/models/{id}")
    @Operation(summary = "Retrieve public layout views of a single specified credit model registration record properties")
    public ResponseEntity<CreditScoringModelResponseDto> getScoringModel(@PathVariable("id") String id) {
        return ResponseEntity.ok(managementService.getModelById(id));
    }

    @DeleteMapping("/models/{id}")
    @Operation(summary = "Logically de-activate an operational scorecard model strategy target path from routing registries")
    public ResponseEntity<Void> deleteScoringModel(@PathVariable("id") String id) {
        managementService.deleteModelLogical(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/models")
    @Operation(summary = "Fetch and filter scorecard model configurations")
    public ResponseEntity<List<CreditScoringModelResponseDto>> getAllScoringModels(
            @RequestParam(value = "partnerId", required = false) String partnerId,
            @RequestParam(value = "isActive", required = false) Boolean isActive) {
        return ResponseEntity.ok(managementService.getAllModels(partnerId, isActive));
    }
}