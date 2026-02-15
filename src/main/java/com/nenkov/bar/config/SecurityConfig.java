package com.nenkov.bar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

  @Bean
  public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

    return http
        // Stateless API: no cookies/session state
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authorizeExchange(
            exchanges ->
                exchanges
                    // allow login endpoint
                    .pathMatchers(HttpMethod.POST, "/auth/login")
                    .permitAll()

                    // allow liveness/readiness
                    .pathMatchers("/actuator/health/**")
                    .permitAll()

                    // allow OpenAPI/Swagger docs
                    .pathMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                    .permitAll()

                    // everything else requires auth
                    .anyExchange()
                    .authenticated())

        // Validate JWT from Authorization: Bearer <token>
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
        .build();
  }
}
