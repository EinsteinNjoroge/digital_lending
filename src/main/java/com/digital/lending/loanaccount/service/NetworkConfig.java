package com.digital.lending.loanaccount.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class NetworkConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // Protect the ledger engine threads from hanging indefinitely
        factory.setConnectTimeout(3000); // 3 seconds connection timeout
        factory.setReadTimeout(5000);    // 5 seconds read timeout

        return new RestTemplate(factory);
    }
}