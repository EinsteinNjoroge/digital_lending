package com.digital.lending.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI globalOpenAPIMetaData() {
        return new OpenAPI()
                .info(new Info()
                        .title("Core Lending Platform API Engine")
                        .version("v1.0.0")
                        .description("Production-grade modular lending core infrastructure engines."));
    }
}