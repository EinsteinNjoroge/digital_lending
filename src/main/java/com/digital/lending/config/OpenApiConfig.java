package com.digital.lending.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI globalOpenApiMetadata() {
        return new OpenAPI()
                .info(new Info()
                        .title("Digital Lending Platform API")
                        .version("v1.0.0")
                        .description("Backend API for loan products, customer onboarding, underwriting, servicing, payments, and notifications."));
    }
}
