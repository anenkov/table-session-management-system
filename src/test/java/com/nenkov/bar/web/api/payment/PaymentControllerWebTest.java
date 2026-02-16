package com.nenkov.bar.web.api.payment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.nenkov.bar.application.payment.exception.CheckCreationNotAllowedException;
import com.nenkov.bar.application.payment.exception.CheckNotFoundException;
import com.nenkov.bar.application.payment.exception.InvalidPaymentSelectionException;
import com.nenkov.bar.application.payment.exception.PaymentRequestIdConflictException;
import com.nenkov.bar.application.payment.model.CreateCheckInput;
import com.nenkov.bar.application.payment.model.CreateCheckResult;
import com.nenkov.bar.application.payment.model.PaymentAttemptResult;
import com.nenkov.bar.application.payment.model.PaymentRequestId;
import com.nenkov.bar.application.payment.model.RecordPaymentAttemptInput;
import com.nenkov.bar.application.payment.model.RecordPaymentAttemptResult;
import com.nenkov.bar.application.payment.service.PaymentService;
import com.nenkov.bar.application.session.exception.TableSessionNotFoundException;
import com.nenkov.bar.auth.jwt.JwtService;
import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.payment.CheckId;
import com.nenkov.bar.domain.model.session.TableSessionId;
import com.nenkov.bar.web.api.error.model.ApiProblemCode;
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

  @Test
  void recordPaymentAttempt_returns200AndResponse() {
    String sessionId = "S-1";
    String requestId = "req-1";
    String checkId = "11111111-1111-1111-1111-111111111111";

    RecordPaymentAttemptResult result =
        new RecordPaymentAttemptResult(
            PaymentRequestId.of(requestId),
            TableSessionId.of(sessionId),
            CheckId.of(UUID.fromString(checkId)),
            PaymentAttemptResult.approved("provider-ref-1"));

    when(paymentService.recordPaymentAttempt(any(RecordPaymentAttemptInput.class)))
        .thenReturn(result);

    webTestClient
        .post()
        .uri("/sessions/{sessionId}/checks/{checkId}/attempts", sessionId, checkId)
        .header(HttpHeaders.AUTHORIZATION, bearerToken)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new RecordPaymentAttemptRequest(requestId))
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.requestId")
        .isEqualTo(requestId)
        .jsonPath("$.sessionId")
        .isEqualTo(sessionId)
        .jsonPath("$.checkId")
        .isEqualTo(checkId)
        .jsonPath("$.attempt.status")
        .isEqualTo("APPROVED")
        .jsonPath("$.attempt.providerReference")
        .isEqualTo("provider-ref-1")
        .jsonPath("$.attempt.failureReason")
        .doesNotExist();
  }

  @Test
  void recordPaymentAttempt_withoutJwt_returns401() {
    webTestClient
        .post()
        .uri(
            "/sessions/{sessionId}/checks/{checkId}/attempts",
            "S-1",
            "11111111-1111-1111-1111-111111111111")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new RecordPaymentAttemptRequest("req-1"))
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void recordPaymentAttempt_validationError_returns400ProblemDetail() {
    webTestClient
        .post()
        .uri(
            "/sessions/{sessionId}/checks/{checkId}/attempts",
            "S-1",
            "11111111-1111-1111-1111-111111111111")
        .header(HttpHeaders.AUTHORIZATION, bearerToken)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new RecordPaymentAttemptRequest(""))
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.code")
        .isEqualTo(ApiProblemCode.VALIDATION_FAILED.name());
  }

  @Test
  void recordPaymentAttempt_invalidSessionId_returns400ProblemDetail() {
    webTestClient
        .post()
        .uri(
            "/sessions/{sessionId}/checks/{checkId}/attempts",
            " ",
            "11111111-1111-1111-1111-111111111111")
        .header(HttpHeaders.AUTHORIZATION, bearerToken)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new RecordPaymentAttemptRequest("req-1"))
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.code")
        .isEqualTo("HTTP_400");
  }

  @Test
  void recordPaymentAttempt_invalidCheckId_returns400ProblemDetail() {
    webTestClient
        .post()
        .uri("/sessions/{sessionId}/checks/{checkId}/attempts", "S-1", "bad-check-id")
        .header(HttpHeaders.AUTHORIZATION, bearerToken)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new RecordPaymentAttemptRequest("req-1"))
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.code")
        .isEqualTo("HTTP_400");
  }

  @Test
  void recordPaymentAttempt_sessionNotFound_returns404ProblemDetail() {
    when(paymentService.recordPaymentAttempt(any(RecordPaymentAttemptInput.class)))
        .thenThrow(new TableSessionNotFoundException(TableSessionId.of("S-404")));

    webTestClient
        .post()
        .uri(
            "/sessions/{sessionId}/checks/{checkId}/attempts",
            "S-404",
            "11111111-1111-1111-1111-111111111111")
        .header(HttpHeaders.AUTHORIZATION, bearerToken)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new RecordPaymentAttemptRequest("req-1"))
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.code")
        .isEqualTo(ApiProblemCode.SESSION_NOT_FOUND.name());
  }

  @Test
  void recordPaymentAttempt_checkNotFound_returns404ProblemDetail() {
    when(paymentService.recordPaymentAttempt(any(RecordPaymentAttemptInput.class)))
        .thenThrow(
            new CheckNotFoundException(
                CheckId.of(UUID.fromString("11111111-1111-1111-1111-111111111111"))));

    webTestClient
        .post()
        .uri(
            "/sessions/{sessionId}/checks/{checkId}/attempts",
            "S-1",
            "11111111-1111-1111-1111-111111111111")
        .header(HttpHeaders.AUTHORIZATION, bearerToken)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new RecordPaymentAttemptRequest("req-1"))
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.code")
        .isEqualTo(ApiProblemCode.CHECK_NOT_FOUND.name());
  }

  @Test
  void recordPaymentAttempt_requestIdConflict_returns409ProblemDetail() {
    when(paymentService.recordPaymentAttempt(any(RecordPaymentAttemptInput.class)))
        .thenThrow(
            new PaymentRequestIdConflictException(
                PaymentRequestId.of("req-1"),
                TableSessionId.of("S-1"),
                CheckId.of(UUID.fromString("11111111-1111-1111-1111-111111111111")),
                TableSessionId.of("S-2"),
                CheckId.of(UUID.fromString("22222222-2222-2222-2222-222222222222"))));

    webTestClient
        .post()
        .uri(
            "/sessions/{sessionId}/checks/{checkId}/attempts",
            "S-1",
            "11111111-1111-1111-1111-111111111111")
        .header(HttpHeaders.AUTHORIZATION, bearerToken)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new RecordPaymentAttemptRequest("req-1"))
        .exchange()
        .expectStatus()
        .isEqualTo(409)
        .expectBody()
        .jsonPath("$.code")
        .isEqualTo(ApiProblemCode.PAYMENT_REQUEST_CONFLICT.name());
  }

  @Test
  void recordPaymentAttempt_invalidRequestId_returns400ProblemDetail() {
    webTestClient
        .post()
        .uri(
            "/sessions/{sessionId}/checks/{checkId}/attempts",
            "S-1",
            "11111111-1111-1111-1111-111111111111")
        .header(HttpHeaders.AUTHORIZATION, bearerToken)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"requestId\":\"   \"}")
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.detail")
        .isEqualTo("Request validation failed.");
  }
}
