package com.nenkov.bar.web.api.error.mapping;

import com.nenkov.bar.auth.InvalidCredentialsException;
import com.nenkov.bar.web.api.error.model.ApiProblemCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

@Component
public class InvalidCredentialsExceptionMapper
    implements ApiExceptionMapper<InvalidCredentialsException> {

  @Override
  public Class<InvalidCredentialsException> type() {
    return InvalidCredentialsException.class;
  }

  @Override
  public ApiProblemCode code() {
    return ApiProblemCode.AUTH_INVALID_CREDENTIALS;
  }

  @Override
  public String safeDetail(InvalidCredentialsException exception, ServerWebExchange exchange) {
    return "Invalid username or password.";
  }
}
