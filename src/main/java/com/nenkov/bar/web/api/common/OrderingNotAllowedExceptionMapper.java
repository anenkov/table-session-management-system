package com.nenkov.bar.web.api.common;

import com.nenkov.bar.domain.exceptions.OrderingNotAllowedException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

@Component
public class OrderingNotAllowedExceptionMapper
    implements ApiExceptionMapper<OrderingNotAllowedException> {

  @Override
  public Class<OrderingNotAllowedException> type() {
    return OrderingNotAllowedException.class;
  }

  @Override
  public ApiProblemCode code() {
    return ApiProblemCode.ORDERING_CONFLICT;
  }

  @Override
  public String safeDetail(OrderingNotAllowedException exception, ServerWebExchange exchange) {
    return "Ordering is not allowed for the current session state.";
  }
}
