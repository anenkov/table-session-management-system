package com.nenkov.bar.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  private static final String BEARER_AUTH = "bearerAuth";

  @Bean
  public OpenAPI openAPI() {
    SecurityScheme bearerScheme =
        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT");

    return new OpenAPI()
        .info(
            new Info()
                .title("Table Session Management System API")
                .description("Reactive API for table sessions, ordering, and payment workflows.")
                .version("v1"))
        .components(new Components().addSecuritySchemes(BEARER_AUTH, bearerScheme))
        .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
  }
}
