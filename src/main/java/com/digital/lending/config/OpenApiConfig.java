package com.digital.lending.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI globalOpenApiMetadata() {
        return new OpenAPI()
                .info(new Info()
                        .title("Core Lending Platform API Engine")
                        .version("v1.0.0")
                        .description("Production-grade modular lending core infrastructure engines."));
    }
}
