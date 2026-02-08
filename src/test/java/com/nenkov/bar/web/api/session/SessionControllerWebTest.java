package com.nenkov.bar.web.api.session;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.nenkov.bar.application.session.exception.TableAlreadyHasOpenSessionException;
import com.nenkov.bar.application.session.exception.TableSessionNotFoundException;
import com.nenkov.bar.application.session.model.GetTableSessionInput;
import com.nenkov.bar.application.session.model.GetTableSessionResult;
import com.nenkov.bar.application.session.model.OpenTableSessionInput;
import com.nenkov.bar.application.session.model.OpenTableSessionResult;
import com.nenkov.bar.application.session.repository.TableSessionRepository;
import com.nenkov.bar.application.session.service.TableSessionService;
import com.nenkov.bar.auth.jwt.JwtService;
import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.session.OrderItemId;
import com.nenkov.bar.domain.model.session.TableSessionId;
import com.nenkov.bar.domain.model.writeoff.ItemWriteOff;
import com.nenkov.bar.domain.model.writeoff.WriteOff;
import com.nenkov.bar.domain.model.writeoff.WriteOffReason;
import com.nenkov.bar.domain.service.payment.SessionItemSnapshot;
import com.nenkov.bar.web.api.common.ApiProblemCode;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Web-layer tests for {@link SessionController}.
 *
 * <p>Conventions: full Spring context, RANDOM_PORT, WebTestClient bound manually, mocked boundaries
 * using {@link MockitoBean}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SessionControllerWebTest {

  @LocalServerPort int port;

  private WebTestClient webTestClient;

  @Autowired JwtService jwtService;

  @MockitoBean TableSessionService tableSessionService;

  // Override the placeholder bean so it never executes in tests.
  @MockitoBean TableSessionRepository tableSessionRepository;

  private String bearerToken;

  @BeforeEach
  void setUp() {
    this.webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    this.bearerToken = "Bearer " + jwtService.generateAccessToken("test-user", "MANAGER");
  }

  @Test
  void openSession_returns201AndResponse() {
    OpenSessionRequest request = new OpenSessionRequest("T-1");

    when(tableSessionService.open(any(OpenTableSessionInput.class)))
        .thenReturn(new OpenTableSessionResult("s-1", "T-1"));

    webTestClient
        .post()
        .uri("/sessions")
        .header("Authorization", bearerToken)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isCreated()
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.sessionId")
        .isEqualTo("s-1")
        .jsonPath("$.tableId")
        .isEqualTo("T-1");
  }

  @Test
  void openSession_validationFailure_returnsProblemDetails400() {
    OpenSessionRequest request = new OpenSessionRequest("");

    webTestClient
        .post()
        .uri("/sessions")
        .header("Authorization", bearerToken)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.valueOf("application/problem+json"))
        .expectBody()
        .jsonPath("$.type")
        .isEqualTo(ApiProblemCode.VALIDATION_FAILED.typeUri().toString())
        .jsonPath("$.title")
        .isEqualTo(ApiProblemCode.VALIDATION_FAILED.title())
        .jsonPath("$.status")
        .isEqualTo(400)
        .jsonPath("$.code")
        .isEqualTo(ApiProblemCode.VALIDATION_FAILED.name())
        .jsonPath("$.errors.length()")
        .isEqualTo(1)
        .jsonPath("$.errors[0].field")
        .isEqualTo("tableId");
  }

  @Test
  void openSession_conflict_returnsProblemDetails409() {
    OpenSessionRequest request = new OpenSessionRequest("T-1");

    when(tableSessionService.open(any(OpenTableSessionInput.class)))
        .thenThrow(new TableAlreadyHasOpenSessionException("T-1"));

    webTestClient
        .post()
        .uri("/sessions")
        .header("Authorization", bearerToken)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isEqualTo(409)
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.valueOf("application/problem+json"))
        .expectBody()
        .jsonPath("$.type")
        .isEqualTo(ApiProblemCode.SESSION_ALREADY_OPEN_FOR_TABLE.typeUri().toString())
        .jsonPath("$.title")
        .isEqualTo(ApiProblemCode.SESSION_ALREADY_OPEN_FOR_TABLE.title())
        .jsonPath("$.status")
        .isEqualTo(409)
        .jsonPath("$.detail")
        .isEqualTo("An open session already exists for this table.")
        .jsonPath("$.code")
        .isEqualTo(ApiProblemCode.SESSION_ALREADY_OPEN_FOR_TABLE.name())
        .jsonPath("$.timestamp")
        .exists();
  }

  @Test
  void openSession_conflict_includesCorrelationIdWhenHeaderProvided() {
    OpenSessionRequest request = new OpenSessionRequest("T-1");

    when(tableSessionService.open(any(OpenTableSessionInput.class)))
        .thenThrow(new TableAlreadyHasOpenSessionException("T-1"));

    webTestClient
        .post()
        .uri("/sessions")
        .header("Authorization", bearerToken)
        .header("X-Request-Id", "abc-123")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isEqualTo(409)
        .expectBody()
        .jsonPath("$.correlationId")
        .isEqualTo("abc-123");
  }

  @Test
  void getSession_returns200AndResponse() {
    GetTableSessionResult result =
        new GetTableSessionResult(TableSessionId.of("s-1"), "EUR", List.of(), List.of(), List.of());

    when(tableSessionService.getById(any(GetTableSessionInput.class))).thenReturn(result);

    webTestClient
        .get()
        .uri("/sessions/{id}", "s-1")
        .header("Authorization", bearerToken)
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.sessionId")
        .isEqualTo("s-1")
        .jsonPath("$.currency")
        .isEqualTo("EUR")
        .jsonPath("$.payableItems.length()")
        .isEqualTo(0)
        .jsonPath("$.itemWriteOffs.length()")
        .isEqualTo(0)
        .jsonPath("$.sessionWriteOffs.length()")
        .isEqualTo(0);
  }

  @Test
  void getSession_returns200AndMapsPayablesAndWriteOffs() {
    OrderItemId orderItemId =
        OrderItemId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));

    Money unitPrice = Money.of("EUR", new BigDecimal("12.30"));
    Money itemWriteOffAmount = Money.of("EUR", new BigDecimal("1.20"));
    Money sessionWriteOffAmount = Money.of("EUR", new BigDecimal("0.10"));

    SessionItemSnapshot payable = new SessionItemSnapshot(orderItemId, unitPrice, 2);

    ItemWriteOff itemWriteOff =
        ItemWriteOff.of(orderItemId, 1, itemWriteOffAmount, WriteOffReason.DISCOUNT, "note-1");

    WriteOff sessionWriteOff = WriteOff.of(sessionWriteOffAmount, WriteOffReason.OTHER, "note-2");

    GetTableSessionResult result =
        new GetTableSessionResult(
            TableSessionId.of("s-1"),
            "EUR",
            List.of(payable),
            List.of(itemWriteOff),
            List.of(sessionWriteOff));

    when(tableSessionService.getById(any(GetTableSessionInput.class))).thenReturn(result);

    webTestClient
        .get()
        .uri("/sessions/{id}", "s-1")
        .header("Authorization", bearerToken)
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .expectBody()
        // Payable mapping (itemId UUID -> string; toMoney uses toPlainString)
        .jsonPath("$.payableItems.length()")
        .isEqualTo(1)
        .jsonPath("$.payableItems[0].itemId")
        .isEqualTo("00000000-0000-0000-0000-000000000001")
        .jsonPath("$.payableItems[0].remainingQuantity")
        .isEqualTo(2)
        .jsonPath("$.payableItems[0].unitPrice.amount")
        .isEqualTo("12.30")
        .jsonPath("$.payableItems[0].unitPrice.currency")
        .isEqualTo("EUR")
        // Item write-off mapping
        .jsonPath("$.itemWriteOffs.length()")
        .isEqualTo(1)
        .jsonPath("$.itemWriteOffs[0].itemId")
        .isEqualTo("00000000-0000-0000-0000-000000000001")
        .jsonPath("$.itemWriteOffs[0].quantity")
        .isEqualTo(1)
        .jsonPath("$.itemWriteOffs[0].amount.amount")
        .isEqualTo("1.20")
        .jsonPath("$.itemWriteOffs[0].amount.currency")
        .isEqualTo("EUR")
        .jsonPath("$.itemWriteOffs[0].reason")
        .isEqualTo("DISCOUNT")
        .jsonPath("$.itemWriteOffs[0].note")
        .isEqualTo("note-1")
        // Session write-off mapping
        .jsonPath("$.sessionWriteOffs.length()")
        .isEqualTo(1)
        .jsonPath("$.sessionWriteOffs[0].amount.amount")
        .isEqualTo("0.10")
        .jsonPath("$.sessionWriteOffs[0].amount.currency")
        .isEqualTo("EUR")
        .jsonPath("$.sessionWriteOffs[0].reason")
        .isEqualTo("OTHER")
        .jsonPath("$.sessionWriteOffs[0].note")
        .isEqualTo("note-2");
  }

  @Test
  void getSession_invalidSessionId_returnsProblemDetails400() {
    // TableSessionId.of rejects blank values; a single space is blank after trimming rules.
    webTestClient
        .get()
        .uri("/sessions/{id}", " ")
        .header("Authorization", bearerToken)
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.valueOf("application/problem+json"))
        .expectBody()
        .jsonPath("$.type")
        .isEqualTo(ApiProblemCode.RESPONSE_STATUS.typeUri().toString())
        .jsonPath("$.code")
        .isEqualTo("HTTP_400")
        .jsonPath("$.status")
        .isEqualTo(400)
        .jsonPath("$.detail")
        .isEqualTo("Invalid sessionId.");
  }

  @Test
  void getSession_notFound_returnsProblemDetails404() {
    TableSessionId id = TableSessionId.of("s-1");

    when(tableSessionService.getById(any(GetTableSessionInput.class)))
        .thenThrow(new TableSessionNotFoundException(id));

    webTestClient
        .get()
        .uri("/sessions/{id}", "s-1")
        .header("Authorization", bearerToken)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.valueOf("application/problem+json"))
        .expectBody()
        .jsonPath("$.type")
        .isEqualTo(ApiProblemCode.SESSION_NOT_FOUND.typeUri().toString())
        .jsonPath("$.title")
        .isEqualTo(ApiProblemCode.SESSION_NOT_FOUND.title())
        .jsonPath("$.status")
        .isEqualTo(404)
        .jsonPath("$.detail")
        .isEqualTo("Session was not found.")
        .jsonPath("$.code")
        .isEqualTo(ApiProblemCode.SESSION_NOT_FOUND.name())
        .jsonPath("$.timestamp")
        .exists();
  }

  @Test
  void openSession_withoutAuth_returns401() {
    webTestClient
        .post()
        .uri("/sessions")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new OpenSessionRequest("T-1"))
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }
}
