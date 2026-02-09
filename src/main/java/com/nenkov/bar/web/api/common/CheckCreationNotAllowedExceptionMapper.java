package com.nenkov.bar.web.api.common;

import com.nenkov.bar.application.payment.exception.CheckCreationNotAllowedException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

@Component
public class CheckCreationNotAllowedExceptionMapper
    implements ApiExceptionMapper<CheckCreationNotAllowedException> {

  @Override
  public Class<CheckCreationNotAllowedException> type() {
    return CheckCreationNotAllowedException.class;
  }

  @Override
  public ApiProblemCode code() {
    return ApiProblemCode.PAYMENT_CONFLICT;
  }

  @Override
  public String safeDetail(CheckCreationNotAllowedException exception, ServerWebExchange exchange) {
    return "Check creation is not allowed for the current session state.";
  }
}
