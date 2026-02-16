package com.nenkov.bar.web.api.conversion;

import com.nenkov.bar.application.payment.model.PaymentRequestId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Converts external string values to {@link PaymentRequestId}.
 *
 * <p>Used for non-path HTTP inputs (for example request-body idempotency keys) to keep ID parsing
 * consistent across the web layer.
 */
@Component
public final class StringToPaymentRequestIdConverter
    implements Converter<String, PaymentRequestId> {

  @Override
  public PaymentRequestId convert(String source) {
    try {
      return PaymentRequestId.of(source);
    } catch (RuntimeException _) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid requestId.");
    }
  }
}

