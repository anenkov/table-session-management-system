package com.nenkov.bar.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecuritySmokeTest {

  @LocalServerPort
  private int port;

  private WebTestClient webTestClient;

  @BeforeEach
  void setUp() {
    this.webTestClient = WebTestClient.bindToServer()
        .baseUrl("http://localhost:" + port)
        .build();
  }

  @Test
  void loginShouldNotRequireAuthentication() {
    webTestClient.post()
        .uri("/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        // invalid payload -> triggers validation -> 400, without DB access
        .bodyValue("{\"username\":\"\",\"password\":\"\"}")
        .exchange()
        .expectStatus().isBadRequest();
  }

  @Test
  void protectedEndpointShouldReturn401WithoutToken() {
    webTestClient.get()
        .uri("/__test/protected")
        .exchange()
        .expectStatus().isUnauthorized();
  }
}
