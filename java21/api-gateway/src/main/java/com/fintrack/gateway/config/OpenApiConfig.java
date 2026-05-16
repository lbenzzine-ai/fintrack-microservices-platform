package com.fintrack.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Root OpenAPI bean for the Gateway's own /v3/api-docs. The aggregated Swagger UI is
 * configured via {@code springdoc.swagger-ui.urls} in {@code api-gateway.yml}, which lists
 * each downstream service's docs reachable through the {@code /aggregate/<svc>/v3/api-docs} routes.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearer-jwt";

    @Bean
    public OpenAPI gatewayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FinTrack API Gateway")
                        .description("Aggregated gateway for FinTrack microservices")
                        .version("1.0.0")
                        .contact(new Contact().name("FinTrack Platform").email("platform@fintrack.io"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("HS256 JWT issued by user-service. Use Bearer prefix.")));
    }
}
