package com.digital.lending.config;

import com.digital.lending.customer.util.CustomerApiConstants;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
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

    @Bean
    public GroupedOpenApi customerModuleApi() {
        return GroupedOpenApi.builder()
                .group(CustomerApiConstants.GROUP_NAME)
                .pathsToMatch(CustomerApiConstants.BASE_PATH + "/**")
                .build();
    }
}