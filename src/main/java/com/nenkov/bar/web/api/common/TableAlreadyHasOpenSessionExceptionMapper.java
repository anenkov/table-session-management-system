package com.nenkov.bar.web.api.common;

import com.nenkov.bar.application.session.exception.TableAlreadyHasOpenSessionException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

@Component
public class TableAlreadyHasOpenSessionExceptionMapper
    implements ApiExceptionMapper<TableAlreadyHasOpenSessionException> {

  @Override
  public Class<TableAlreadyHasOpenSessionException> type() {
    return TableAlreadyHasOpenSessionException.class;
  }

  @Override
  public ApiProblemCode code() {
    return ApiProblemCode.SESSION_ALREADY_OPEN_FOR_TABLE;
  }

  @Override
  public String safeDetail(
      TableAlreadyHasOpenSessionException exception, ServerWebExchange exchange) {
    return "An open session already exists for this table.";
  }
}
