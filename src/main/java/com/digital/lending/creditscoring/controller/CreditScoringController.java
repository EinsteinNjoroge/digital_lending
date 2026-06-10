package com.digital.lending.creditscoring.controller;

import com.digital.lending.creditscoring.dto.CreditDecisionResponse;
import com.digital.lending.creditscoring.dto.CreditScoringModelRequestDto;
import com.digital.lending.creditscoring.dto.CreditScoringModelResponseDto;
import com.digital.lending.creditscoring.dto.ScoringRequestDto;
import com.digital.lending.creditscoring.service.CreditScoringModelManagementService;
import com.digital.lending.creditscoring.service.CreditScoringOrchestrationEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/credit-scoring")
@RequiredArgsConstructor
@Tag(name = "Credit Scoring Domain Engine", description = "Multi-tenant rule evaluation orchestrator handling polymorphic underwriting frameworks for thin-file profile segments.")
public class CreditScoringController {

    private final CreditScoringOrchestrationEngine orchestrationEngine;
    private final CreditScoringModelManagementService managementService;

    @PostMapping("/evaluate")
    @Operation(
            summary = "Evaluate loan applications against dynamic scorecard rules",
            description = "Processes incoming profile telemetry and application context against tenant-level scorecard models through the in-process underwriting engine."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CreditDecisionResponse.class))),
            @ApiResponse(responseCode = "400", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<CreditDecisionResponse> evaluateProfileRisk(@Valid @RequestBody ScoringRequestDto request) {
        return ResponseEntity.ok(orchestrationEngine.resolveAndEvaluate(request));
    }

    @PostMapping("/models")
    @Operation(
            summary = "Provision a new multi-tenant credit scorecard rule matrix model definition record",
            description = "Creates a credit scoring model."
    )
    public ResponseEntity<CreditScoringModelResponseDto> createScoringModel(
            @Valid @RequestBody CreditScoringModelRequestDto request) {
        return ResponseEntity.ok(managementService.createModel(request));
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
