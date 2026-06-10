package com.digital.lending.loanaccount.controller;

import com.digital.lending.loanaccount.dto.LoanServicingRunResponseDto;
import com.digital.lending.loanaccount.service.LoanServicingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/loan-accounts/servicing")
@RequiredArgsConstructor
public class LoanServicingController {

    private final LoanServicingService loanServicingService;

    @PostMapping("/run")
    public ResponseEntity<LoanServicingRunResponseDto> runServicing(
            @RequestParam(value = "trigger", defaultValue = "manual") String trigger) {
        return ResponseEntity.ok(loanServicingService.runServicing(trigger));
    }
}
