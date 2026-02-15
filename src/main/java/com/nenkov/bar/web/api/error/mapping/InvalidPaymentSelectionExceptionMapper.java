package com.nenkov.bar.web.api.error.mapping;

import com.nenkov.bar.application.payment.exception.InvalidPaymentSelectionException;
import com.nenkov.bar.web.api.error.model.ApiProblemCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

@Component
public class InvalidPaymentSelectionExceptionMapper
    implements ApiExceptionMapper<InvalidPaymentSelectionException> {

  @Override
  public Class<InvalidPaymentSelectionException> type() {
    return InvalidPaymentSelectionException.class;
  }

  @Override
  public ApiProblemCode code() {
    return ApiProblemCode.PAYMENT_SELECTION_INVALID;
  }

  @Override
  public String safeDetail(InvalidPaymentSelectionException exception, ServerWebExchange exchange) {
    return "Invalid payment selection.";
  }
}
