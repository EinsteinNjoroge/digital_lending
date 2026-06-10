package com.digital.lending.loanaccount.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.servicing.enabled", havingValue = "true")
public class LoanServicingScheduler {

    private final RestClient restClient;
    private final String servicingBaseUrl;

    public LoanServicingScheduler(
            RestClient.Builder restClientBuilder,
            @Value("${app.servicing.base-url:http://localhost:8080}") String servicingBaseUrl) {
        this.restClient = restClientBuilder.build();
        this.servicingBaseUrl = servicingBaseUrl;
    }

    @Scheduled(cron = "${app.servicing.cron:0 0 * * * *}")
    public void triggerServicingRun() {
        String endpoint = servicingBaseUrl + "/api/v1/internal/loan-accounts/servicing/run?trigger=cron";
        try {
            restClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Triggered scheduled servicing run via {}", endpoint);
        } catch (Exception ex) {
            log.error("Failed to trigger scheduled servicing run via {}", endpoint, ex);
        }
    }
}
