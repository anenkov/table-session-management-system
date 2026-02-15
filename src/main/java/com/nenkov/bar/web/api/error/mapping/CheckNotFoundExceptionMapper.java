package com.nenkov.bar.web.api.error.mapping;

import com.nenkov.bar.application.payment.exception.CheckNotFoundException;
import com.nenkov.bar.web.api.error.model.ApiProblemCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

@Component
public class CheckNotFoundExceptionMapper implements ApiExceptionMapper<CheckNotFoundException> {

  @Override
  public Class<CheckNotFoundException> type() {
    return CheckNotFoundException.class;
  }

  @Override
  public ApiProblemCode code() {
    return ApiProblemCode.CHECK_NOT_FOUND;
  }

  @Override
  public String safeDetail(CheckNotFoundException exception, ServerWebExchange exchange) {
    return "Check was not found.";
  }
}
