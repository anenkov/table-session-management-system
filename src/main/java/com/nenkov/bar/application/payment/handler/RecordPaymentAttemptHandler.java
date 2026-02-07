package com.nenkov.bar.application.payment.handler;

import com.nenkov.bar.application.payment.exception.CheckNotFoundException;
import com.nenkov.bar.application.payment.exception.PaymentRequestIdConflictException;
import com.nenkov.bar.application.payment.gateway.PaymentGateway;
import com.nenkov.bar.application.payment.model.PaymentAttemptResult;
import com.nenkov.bar.application.payment.model.PaymentAttemptStatus;
import com.nenkov.bar.application.payment.model.RecordPaymentAttemptInput;
import com.nenkov.bar.application.payment.model.RecordPaymentAttemptResult;
import com.nenkov.bar.application.payment.model.RecordedPaymentAttempt;
import com.nenkov.bar.application.payment.repository.CheckRepository;
import com.nenkov.bar.application.payment.repository.PaymentAttemptRepository;
import com.nenkov.bar.domain.model.payment.Check;
import com.nenkov.bar.domain.model.payment.PaymentReference;
import java.time.Instant;
import java.util.Objects;

/**
 * Workflow handler: initiate a payment attempt through the payment gateway and update check status.
 *
 * <p>State mapping:
 *
 * <ul>
 *   <li>APPROVED -> markPaid(reference, now) and save
 *   <li>DECLINED -> markFailed(now) and save
 *   <li>PENDING -> no state change (check remains non-terminal)
 * </ul>
 */
public final class RecordPaymentAttemptHandler {

  private final PaymentGateway paymentGateway;
  private final CheckRepository checkRepository;
  private final PaymentAttemptRepository paymentAttemptRepository;

  public RecordPaymentAttemptHandler(
      PaymentGateway paymentGateway,
      CheckRepository checkRepository,
      PaymentAttemptRepository paymentAttemptRepository) {
    this.paymentGateway = Objects.requireNonNull(paymentGateway, "paymentGateway must not be null");
    this.checkRepository =
        Objects.requireNonNull(checkRepository, "checkRepository must not be null");
    this.paymentAttemptRepository =
        Objects.requireNonNull(
            paymentAttemptRepository, "paymentAttemptRepository must not be null");
  }

  public RecordPaymentAttemptResult handle(RecordPaymentAttemptInput input) {
    Objects.requireNonNull(input, "input must not be null");

    // Idempotency: if we have already processed this request id, return the stored outcome.
    // This prevents duplicate gateway calls and duplicate state transitions/writes.
    RecordedPaymentAttempt existing =
        paymentAttemptRepository.findByRequestId(input.requestId()).orElse(null);
    if (existing != null) {
      if (!existing.sessionId().equals(input.sessionId())
          || !existing.checkId().equals(input.checkId())) {
        throw new PaymentRequestIdConflictException(
            input.requestId(),
            existing.sessionId(),
            existing.checkId(),
            input.sessionId(),
            input.checkId());
      }
      return new RecordPaymentAttemptResult(
          input.requestId(), input.sessionId(), input.checkId(), existing.attemptResult());
    }

    Check check =
        checkRepository
            .findById(input.checkId())
            .orElseThrow(() -> new CheckNotFoundException(input.checkId()));

    PaymentAttemptResult attempt =
        paymentGateway.initiatePayment(
            input.requestId(), input.sessionId(), input.checkId(), check.amount());

    applyOutcome(check, attempt);

    paymentAttemptRepository.save(
        new RecordedPaymentAttempt(input.requestId(), input.sessionId(), input.checkId(), attempt));
    return new RecordPaymentAttemptResult(
        input.requestId(), input.sessionId(), input.checkId(), attempt);
  }

  private void applyOutcome(Check check, PaymentAttemptResult attempt) {
    Instant now = Instant.now();
    PaymentAttemptStatus status = attempt.status();

    switch (status) {
      case APPROVED -> {
        check.markPaid(PaymentReference.of(attempt.providerReference()), now);
        checkRepository.save(check);
      }
      case DECLINED -> {
        check.markFailed(now);
        checkRepository.save(check);
      }
      case PENDING -> {
        // Intentionally, no state transition.
        // Future: if you introduce AUTHORIZED/PENDING correlation, we can extend the model.
      }
    }
  }
}
