package com.nenkov.bar.web.api.common;

import static org.mockito.Mockito.when;

import com.nenkov.bar.auth.AuthService;
import com.nenkov.bar.auth.InvalidCredentialsException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiExceptionHandlerWebTest {

  @LocalServerPort int port;

  WebTestClient webTestClient;

  @BeforeEach
  void setUp() {
    this.webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
  }

  @MockitoBean AuthService authService;

  @Test
  void validationFailure_returnsProblemDetailsWithFieldErrors() {
    String body =
        """
        {
          "username": "",
          "password": ""
        }
        """;

    webTestClient
        .post()
        .uri("/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.valueOf("application/problem+json"))
        .expectBody()
        .jsonPath("$.type")
        .isNotEmpty()
        .jsonPath("$.title")
        .isEqualTo("Validation failed")
        .jsonPath("$.status")
        .isEqualTo(400)
        .jsonPath("$.detail")
        .isEqualTo("Request validation failed.")
        .jsonPath("$.code")
        .isEqualTo("VALIDATION_FAILED")
        .jsonPath("$.errors")
        .isArray()
        .jsonPath("$.errors")
        .isArray()
        .jsonPath("$.errors.length()")
        .isEqualTo(2)
        .jsonPath("$.errors[*].field")
        .value(
            fields -> {
              @SuppressWarnings("unchecked")
              var list = (java.util.List<String>) fields;

              Assertions.assertThat(list)
                  .isNotNull()
                  .isNotEmpty()
                  .containsExactlyInAnyOrder("username", "password");
            })
        .jsonPath("$.errors[*].issue")
        .value(
            issues -> {
              @SuppressWarnings("unchecked")
              var list = (java.util.List<String>) issues;

              Assertions.assertThat(list)
                  .isNotNull()
                  .isNotEmpty()
                  .allMatch(s -> s != null && !s.isBlank());
            });
  }

  @Test
  void invalidCredentials_returnsProblemDetails401() {
    when(authService.login("user", "pass"))
        .thenReturn(Mono.error(new InvalidCredentialsException()));

    String body =
        """
        {
          "username": "user",
          "password": "pass"
        }
        """;

    webTestClient
        .post()
        .uri("/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.valueOf("application/problem+json"))
        .expectBody()
        .jsonPath("$.type")
        .isNotEmpty()
        .jsonPath("$.title")
        .isEqualTo("Invalid credentials")
        .jsonPath("$.status")
        .isEqualTo(401)
        .jsonPath("$.code")
        .isEqualTo("AUTH_INVALID_CREDENTIALS");
  }

  @Test
  void unexpectedException_returnsProblemDetails500() {
    when(authService.login("user", "pass")).thenReturn(Mono.error(new RuntimeException("boom")));

    String body =
        """
      {
        "username": "user",
        "password": "pass"
      }
      """;

    webTestClient
        .post()
        .uri("/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .exchange()
        .expectStatus()
        .is5xxServerError()
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.valueOf("application/problem+json"))
        .expectBody()
        .jsonPath("$.type")
        .isNotEmpty()
        .jsonPath("$.title")
        .isEqualTo("Internal error")
        .jsonPath("$.status")
        .isEqualTo(500)
        .jsonPath("$.code")
        .isEqualTo("INTERNAL_ERROR");
  }

  @Test
  void responseStatusException_isMappedToProblemDetails() {
    when(authService.login("user", "pass"))
        .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "bad request")));

    String body =
        """
      {
        "username": "user",
        "password": "pass"
      }
      """;

    webTestClient
        .post()
        .uri("/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.valueOf("application/problem+json"))
        .expectBody()
        .jsonPath("$.type")
        .isEqualTo("urn:problem:response-status")
        .jsonPath("$.title")
        .isEqualTo("Bad Request")
        .jsonPath("$.status")
        .isEqualTo(400)
        .jsonPath("$.detail")
        .isEqualTo("bad request")
        .jsonPath("$.code")
        .isEqualTo("HTTP_400");
  }

  @Test
  void loginSuccess_returnsLoginResponse() {
    when(authService.login("user", "pass"))
        .thenReturn(Mono.just(new com.nenkov.bar.web.api.auth.LoginResponse("token-1", 3600)));

    String body =
        """
      {
        "username": "user",
        "password": "pass"
      }
      """;

    webTestClient
        .post()
        .uri("/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.accessToken")
        .isEqualTo("token-1")
        .jsonPath("$.expiresInSeconds")
        .isEqualTo(3600);
  }
}
