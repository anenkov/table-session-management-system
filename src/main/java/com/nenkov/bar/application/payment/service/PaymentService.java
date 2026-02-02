package com.nenkov.bar.application.payment.service;

import com.nenkov.bar.application.payment.model.CreateCheckInput;
import com.nenkov.bar.application.payment.model.CreateCheckResult;
import com.nenkov.bar.application.payment.model.RecordPaymentAttemptInput;
import com.nenkov.bar.application.payment.model.RecordPaymentAttemptResult;

/**
 * Feature façade for payment-related workflows.
 *
 * <p>Thin entry point: delegates to dedicated workflow handlers.
 */
public interface PaymentService {

  CreateCheckResult createCheck(CreateCheckInput input);

  RecordPaymentAttemptResult recordPaymentAttempt(RecordPaymentAttemptInput input);
}
