package com.nenkov.bar.web.api.error.mapping;

import com.nenkov.bar.application.payment.exception.PaymentRequestIdConflictException;
import com.nenkov.bar.web.api.error.model.ApiProblemCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

@Component
public class PaymentRequestIdConflictExceptionMapper
    implements ApiExceptionMapper<PaymentRequestIdConflictException> {

  @Override
  public Class<PaymentRequestIdConflictException> type() {
    return PaymentRequestIdConflictException.class;
  }

  @Override
  public ApiProblemCode code() {
    return ApiProblemCode.PAYMENT_REQUEST_CONFLICT;
  }

  @Override
  public String safeDetail(
      PaymentRequestIdConflictException exception, ServerWebExchange exchange) {
    return "requestId was already used for a different session/check.";
  }
}
