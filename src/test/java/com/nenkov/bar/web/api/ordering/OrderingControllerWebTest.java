package com.nenkov.bar.web.api.ordering;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.nenkov.bar.application.ordering.model.AddOrderItemsInput;
import com.nenkov.bar.application.ordering.model.AddOrderItemsResult;
import com.nenkov.bar.application.ordering.service.OrderingService;
import com.nenkov.bar.application.session.exception.TableSessionNotFoundException;
import com.nenkov.bar.auth.jwt.JwtService;
import com.nenkov.bar.domain.exceptions.OrderingNotAllowedException;
import com.nenkov.bar.domain.model.session.TableSessionId;
import com.nenkov.bar.web.api.error.model.ApiProblemCode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Web-layer tests for {@link OrderingController}.
 *
 * <p>Conventions: full Spring context, RANDOM_PORT, WebTestClient bound manually, mocked boundaries
 * using {@link MockitoBean}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderingControllerWebTest {

  @LocalServerPort int port;

  private WebTestClient webTestClient;

  @Autowired JwtService jwtService;

  @MockitoBean OrderingService orderingService;

  private String bearerToken;

  @BeforeEach
  void setUp() {
    this.webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    this.bearerToken = "Bearer " + jwtService.generateAccessToken("test-user", "MANAGER");
  }

  @Test
  void addOrderItems_returns200AndResponse() {
    AddOrderItemsRequest request =
        new AddOrderItemsRequest(
            List.of(
                new AddOrderItemsRequest.AddOrderItemLine("P-1", 1),
                new AddOrderItemsRequest.AddOrderItemLine("P-1", 2)));

    when(orderingService.addItems(any(AddOrderItemsInput.class)))
        .thenReturn(new AddOrderItemsResult("s-1", List.of("i-1", "i-2")));

    webTestClient
        .post()
        .uri("/sessions/{sessionId}/orders/items", "s-1")
        .header("Authorization", bearerToken)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.sessionId")
        .isEqualTo("s-1")
        .jsonPath("$.createdItemIds.length()")
        .isEqualTo(2)
        .jsonPath("$.createdItemIds[0]")
        .isEqualTo("i-1")
        .jsonPath("$.createdItemIds[1]")
        .isEqualTo("i-2");
  }

  @Test
  void addOrderItems_validationFailure_returnsProblemDetails400() {
    webTestClient
        .post()
        .uri("/sessions/{sessionId}/orders/items", "s-1")
        .header("Authorization", bearerToken)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            """
            {
              "items": [
                { "productId": "", "quantity": 0 }
              ]
            }
            """)
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
        .isEqualTo(2);
  }

  @Test
  void addOrderItems_notFound_returnsProblemDetails404() {
    AddOrderItemsRequest request =
        new AddOrderItemsRequest(List.of(new AddOrderItemsRequest.AddOrderItemLine("P-1", 1)));

    when(orderingService.addItems(any(AddOrderItemsInput.class)))
        .thenThrow(new TableSessionNotFoundException(TableSessionId.of("s-404")));

    webTestClient
        .post()
        .uri("/sessions/{sessionId}/orders/items", "s-404")
        .header("Authorization", bearerToken)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
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
  void addOrderItems_conflict_returnsProblemDetails409() {
    AddOrderItemsRequest request =
        new AddOrderItemsRequest(List.of(new AddOrderItemsRequest.AddOrderItemLine("P-1", 1)));

    when(orderingService.addItems(any(AddOrderItemsInput.class)))
        .thenThrow(new OrderingNotAllowedException("ordering-not-allowed"));

    webTestClient
        .post()
        .uri("/sessions/{sessionId}/orders/items", "s-1")
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
        .isEqualTo(ApiProblemCode.ORDERING_CONFLICT.typeUri().toString())
        .jsonPath("$.title")
        .isEqualTo(ApiProblemCode.ORDERING_CONFLICT.title())
        .jsonPath("$.status")
        .isEqualTo(409)
        .jsonPath("$.detail")
        .isEqualTo("Ordering is not allowed for the current session state.")
        .jsonPath("$.code")
        .isEqualTo(ApiProblemCode.ORDERING_CONFLICT.name())
        .jsonPath("$.timestamp")
        .exists();
  }

  @Test
  void addOrderItems_conflict_includesCorrelationIdWhenHeaderProvided() {
    AddOrderItemsRequest request =
        new AddOrderItemsRequest(List.of(new AddOrderItemsRequest.AddOrderItemLine("P-1", 1)));

    when(orderingService.addItems(any(AddOrderItemsInput.class)))
        .thenThrow(new OrderingNotAllowedException("ordering-not-allowed"));

    webTestClient
        .post()
        .uri("/sessions/{sessionId}/orders/items", "s-1")
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
  void addOrderItems_withoutAuth_returns401() {
    AddOrderItemsRequest request =
        new AddOrderItemsRequest(List.of(new AddOrderItemsRequest.AddOrderItemLine("P-1", 1)));

    webTestClient
        .post()
        .uri("/sessions/{sessionId}/orders/items", "s-1")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }
}
