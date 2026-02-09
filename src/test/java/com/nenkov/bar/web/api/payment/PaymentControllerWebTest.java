package com.nenkov.bar.web.api.payment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.nenkov.bar.application.payment.exception.CheckCreationNotAllowedException;
import com.nenkov.bar.application.payment.exception.InvalidPaymentSelectionException;
import com.nenkov.bar.application.payment.model.CreateCheckInput;
import com.nenkov.bar.application.payment.model.CreateCheckResult;
import com.nenkov.bar.application.payment.service.PaymentService;
import com.nenkov.bar.application.session.exception.TableSessionNotFoundException;
import com.nenkov.bar.auth.jwt.JwtService;
import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.payment.CheckId;
import com.nenkov.bar.domain.model.session.TableSessionId;
import com.nenkov.bar.web.api.common.ApiProblemCode;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentControllerWebTest {

  @LocalServerPort int port;

  private WebTestClient webTestClient;

  @Autowired JwtService jwtService;

  @MockitoBean PaymentService paymentService;

  private String bearerToken;

  @BeforeEach
  void setUp() {
    this.webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    this.bearerToken = "Bearer " + jwtService.generateAccessToken("test-user", "MANAGER");
  }

  @Test
  void createCheck_returns201AndResponse() {
    String sessionId = "S-1";
    String itemId = UUID.randomUUID().toString();

    CreateCheckRequest request =
        new CreateCheckRequest(List.of(new CreateCheckRequest.SelectionLine(itemId, 2)));

    CreateCheckResult result =
        new CreateCheckResult(
            TableSessionId.of(sessionId),
            CheckId.of(UUID.fromString("11111111-1111-1111-1111-111111111111")),
            Money.of("EUR", new BigDecimal("12.34")));

    when(paymentService.createCheck(any(CreateCheckInput.class))).thenReturn(result);

    webTestClient
        .post()
        .uri("/sessions/{sessionId}/checks", sessionId)
        .header(HttpHeaders.AUTHORIZATION, bearerToken)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isCreated()
        .expectHeader()
        .contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.sessionId")
        .isEqualTo(sessionId)
        .jsonPath("$.checkId")
        .isEqualTo("11111111-1111-1111-1111-111111111111")
        .jsonPath("$.amount.amount")
        .isEqualTo("12.34")
        .jsonPath("$.amount.currency")
        .isEqualTo("EUR");
  }

  @Test
  void createCheck_withoutJwt_returns401() {
    webTestClient
        .post()
        .uri("/sessions/{sessionId}/checks", "S-1")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            new CreateCheckRequest(
                List.of(new CreateCheckRequest.SelectionLine(UUID.randomUUID().toString(), 1))))
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void createCheck_validationError_returns400ProblemDetail() {
    CreateCheckRequest request = new CreateCheckRequest(List.of());

    webTestClient
        .post()
        .uri("/sessions/{sessionId}/checks", "S-1")
        .header(HttpHeaders.AUTHORIZATION, bearerToken)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.code")
        .isEqualTo(ApiProblemCode.VALIDATION_FAILED.name());
  }

  @Test
  void createCheck_sessionNotFound_returns404ProblemDetail() {
    when(paymentService.createCheck(any(CreateCheckInput.class)))
        .thenThrow(new TableSessionNotFoundException(TableSessionId.of("S-404")));

    webTestClient
        .post()
        .uri("/sessions/{sessionId}/checks", "S-404")
        .header(HttpHeaders.AUTHORIZATION, bearerToken)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            new CreateCheckRequest(
                List.of(new CreateCheckRequest.SelectionLine(UUID.randomUUID().toString(), 1))))
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.code")
        .isEqualTo(ApiProblemCode.SESSION_NOT_FOUND.name());
  }

  @Test
  void createCheck_conflict_returns409ProblemDetailAndCorrelationId() {
    String correlationId = "req-123";
    when(paymentService.createCheck(any(CreateCheckInput.class)))
        .thenThrow(new CheckCreationNotAllowedException(TableSessionId.of("S-1")));

    webTestClient
        .post()
        .uri("/sessions/{sessionId}/checks", "S-1")
        .header(HttpHeaders.AUTHORIZATION, bearerToken)
        .header("X-Request-Id", correlationId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            new CreateCheckRequest(
                List.of(new CreateCheckRequest.SelectionLine(UUID.randomUUID().toString(), 1))))
        .exchange()
        .expectStatus()
        .isEqualTo(409)
        .expectBody()
        .jsonPath("$.code")
        .isEqualTo(ApiProblemCode.PAYMENT_CONFLICT.name())
        .jsonPath("$.correlationId")
        .isEqualTo(correlationId);
  }

  @Test
  void createCheck_invalidSelection_returns400ProblemDetail() {
    when(paymentService.createCheck(any(CreateCheckInput.class)))
        .thenThrow(new InvalidPaymentSelectionException(TableSessionId.of("S-1")));

    webTestClient
        .post()
        .uri("/sessions/{sessionId}/checks", "S-1")
        .header(HttpHeaders.AUTHORIZATION, bearerToken)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            new CreateCheckRequest(
                List.of(new CreateCheckRequest.SelectionLine(UUID.randomUUID().toString(), 99))))
        .exchange()
        .expectStatus()
        .isEqualTo(400)
        .expectBody()
        .jsonPath("$.code")
        .isEqualTo(ApiProblemCode.PAYMENT_SELECTION_INVALID.name());
  }

  @Test
  void createCheck_invalidSessionId_returns400ProblemDetail() {
    String invalidSessionId = "   ";

    CreateCheckRequest request =
        new CreateCheckRequest(
            List.of(new CreateCheckRequest.SelectionLine(UUID.randomUUID().toString(), 1)));

    webTestClient
        .post()
        .uri("/sessions/{sessionId}/checks", invalidSessionId)
        .header(HttpHeaders.AUTHORIZATION, bearerToken)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.code")
        .isEqualTo("HTTP_400");
  }

  @Test
  void createCheck_invalidItemId_returns400ProblemDetail() {
    String sessionId = "S-1";
    String invalidItemId = "not-a-uuid";

    CreateCheckRequest request =
        new CreateCheckRequest(List.of(new CreateCheckRequest.SelectionLine(invalidItemId, 1)));

    webTestClient
        .post()
        .uri("/sessions/{sessionId}/checks", sessionId)
        .header(HttpHeaders.AUTHORIZATION, bearerToken)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.code")
        .isEqualTo("HTTP_400");
  }
}
