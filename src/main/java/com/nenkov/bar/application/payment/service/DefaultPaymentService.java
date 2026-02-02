package com.nenkov.bar.application.payment.service;

import com.nenkov.bar.application.payment.handler.CreateCheckHandler;
import com.nenkov.bar.application.payment.handler.RecordPaymentAttemptHandler;
import com.nenkov.bar.application.payment.model.CreateCheckInput;
import com.nenkov.bar.application.payment.model.CreateCheckResult;
import com.nenkov.bar.application.payment.model.RecordPaymentAttemptInput;
import com.nenkov.bar.application.payment.model.RecordPaymentAttemptResult;
import java.util.Objects;

/**
 * Default implementation of {@link PaymentService}.
 *
 * <p>Thin façade: delegates each workflow to its handler.
 */
public final class DefaultPaymentService implements PaymentService {

  private final CreateCheckHandler createCheckHandler;
  private final RecordPaymentAttemptHandler recordPaymentAttemptHandler;

  public DefaultPaymentService(
      CreateCheckHandler createCheckHandler,
      RecordPaymentAttemptHandler recordPaymentAttemptHandler) {
    this.createCheckHandler =
        Objects.requireNonNull(createCheckHandler, "createCheckHandler must not be null");
    this.recordPaymentAttemptHandler =
        Objects.requireNonNull(
            recordPaymentAttemptHandler, "recordPaymentAttemptHandler must not be null");
  }

  @Override
  public CreateCheckResult createCheck(CreateCheckInput input) {
    return createCheckHandler.handle(input);
  }

  @Override
  public RecordPaymentAttemptResult recordPaymentAttempt(RecordPaymentAttemptInput input) {
    return recordPaymentAttemptHandler.handle(input);
  }
}
