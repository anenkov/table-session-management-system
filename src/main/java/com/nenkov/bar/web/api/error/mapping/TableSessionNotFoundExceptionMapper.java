package com.nenkov.bar.web.api.error.mapping;

import com.nenkov.bar.application.session.exception.TableSessionNotFoundException;
import com.nenkov.bar.web.api.error.model.ApiProblemCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

@Component
public class TableSessionNotFoundExceptionMapper
    implements ApiExceptionMapper<TableSessionNotFoundException> {

  @Override
  public Class<TableSessionNotFoundException> type() {
    return TableSessionNotFoundException.class;
  }

  @Override
  public ApiProblemCode code() {
    return ApiProblemCode.SESSION_NOT_FOUND;
  }

  @Override
  public String safeDetail(TableSessionNotFoundException exception, ServerWebExchange exchange) {
    return "Session was not found.";
  }
}
