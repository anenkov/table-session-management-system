package com.nenkov.bar.web.api.payment;

import com.nenkov.bar.application.payment.model.CreateCheckInput;
import com.nenkov.bar.application.payment.model.CreateCheckResult;
import com.nenkov.bar.application.payment.model.PaymentAttemptResult;
import com.nenkov.bar.application.payment.model.PaymentRequestId;
import com.nenkov.bar.application.payment.model.RecordPaymentAttemptInput;
import com.nenkov.bar.application.payment.model.RecordPaymentAttemptResult;
import com.nenkov.bar.application.payment.service.PaymentService;
import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.payment.CheckId;
import com.nenkov.bar.domain.model.payment.PaymentSelection;
import com.nenkov.bar.domain.model.session.OrderItemId;
import com.nenkov.bar.domain.model.session.TableSessionId;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * Payment HTTP API.
 *
 * <p>This controller is intentionally thin: it performs request validation and mapping to
 * application-layer inputs, delegates to {@link PaymentService}, and maps only syntactic identifier
 * parsing errors to {@code 400 Bad Request}.
 *
 * <p>Business rule failures are mapped via {@link
 * com.nenkov.bar.web.api.error.handler.ApiExceptionHandler} to RFC7807 Problem Details.
 */
@RestController
@RequestMapping(path = "/sessions", produces = MediaType.APPLICATION_JSON_VALUE)
public final class PaymentController {

  private final PaymentService paymentService;

  public PaymentController(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  /**
   * Creates a new check for the given session by selecting payable items and quantities.
   *
   * <p>HTTP: {@code 201 Created} on success.
   *
   * <p>Selection rules:
   *
   * <ul>
   *   <li>Request validation (non-empty selection, quantity >= 1) is handled via bean validation.
   *   <li>Duplicate itemIds are allowed; the application/domain consolidates selections.
   * </ul>
   */
  @PostMapping(path = "/{sessionId}/checks", consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<CreateCheckResponse> createCheck(
      @PathVariable TableSessionId sessionId, @Valid @RequestBody CreateCheckRequest request) {

    return Mono.fromSupplier(
        () -> {
          List<PaymentSelection> selections =
              request.selections().stream()
                  .map(s -> PaymentSelection.of(parseOrderItemId(s.itemId()), s.quantity()))
                  .toList();

          CreateCheckResult result =
              paymentService.createCheck(new CreateCheckInput(sessionId, selections));

          return new CreateCheckResponse(
              result.sessionId().value(),
              result.checkId().value().toString(),
              toMoney(result.amount()));
        });
  }

  /**
   * Records a payment attempt for an existing check using an idempotency request id.
   *
   * <p>HTTP: {@code 200 OK} on success.
   */
  @PostMapping(
      path = "/{sessionId}/checks/{checkId}/attempts",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<RecordPaymentAttemptResponse> recordPaymentAttempt(
      @PathVariable TableSessionId sessionId,
      @PathVariable CheckId checkId,
      @Valid @RequestBody RecordPaymentAttemptRequest request) {

    return Mono.fromSupplier(
        () -> {
          PaymentRequestId requestId = parseRequestId(request.requestId());

          RecordPaymentAttemptResult result =
              paymentService.recordPaymentAttempt(
                  new RecordPaymentAttemptInput(requestId, sessionId, checkId));

          return new RecordPaymentAttemptResponse(
              result.requestId().value(),
              result.sessionId().value(),
              result.checkId().value().toString(),
              toAttempt(result.attemptResult()));
        });
  }

  /**
   * Parses an order item id from the request body. Invalid values are treated as a client input
   * error and mapped to {@code 400 Bad Request}.
   */
  private static OrderItemId parseOrderItemId(String raw) {
    try {
      return OrderItemId.of(UUID.fromString(raw));
    } catch (RuntimeException _) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid itemId.");
    }
  }

  /**
   * Parses an idempotency request id from the request body. Invalid values are treated as a client
   * input error and mapped to {@code 400 Bad Request}.
   */
  private static PaymentRequestId parseRequestId(String raw) {
    try {
      return PaymentRequestId.of(raw);
    } catch (RuntimeException _) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid requestId.");
    }
  }

  private static RecordPaymentAttemptResponse.Attempt toAttempt(PaymentAttemptResult result) {
    return new RecordPaymentAttemptResponse.Attempt(
        result.status().name(), result.providerReference(), result.failureReason().orElse(null));
  }

  /** Maps domain {@link Money} to API money representation. */
  private static CreateCheckResponse.Money toMoney(Money money) {
    return new CreateCheckResponse.Money(money.amount().toPlainString(), money.currency());
  }
}
