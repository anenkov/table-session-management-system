package com.nenkov.bar.web.api.error;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nenkov.bar.auth.InvalidCredentialsException;
import com.nenkov.bar.web.api.error.mapping.ApiExceptionMapper;
import com.nenkov.bar.web.api.error.mapping.ApiExceptionMapperRegistry;
import com.nenkov.bar.web.api.error.model.ApiProblemCode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ServerWebExchange;

class ApiExceptionMapperRegistryDuplicateTest {

  @Test
  void shouldFailFastWhenDuplicateMappersExistForSameType() {
    ApiExceptionMapper<InvalidCredentialsException> m1 =
        new ApiExceptionMapper<>() {
          @Override
          public Class<InvalidCredentialsException> type() {
            return InvalidCredentialsException.class;
          }

          @Override
          public ApiProblemCode code() {
            return ApiProblemCode.AUTH_INVALID_CREDENTIALS;
          }

          @Override
          public String safeDetail(
              InvalidCredentialsException exception, ServerWebExchange exchange) {
            return "x";
          }
        };

    ApiExceptionMapper<InvalidCredentialsException> m2 =
        new ApiExceptionMapper<>() {
          @Override
          public Class<InvalidCredentialsException> type() {
            return InvalidCredentialsException.class;
          }

          @Override
          public ApiProblemCode code() {
            return ApiProblemCode.AUTH_INVALID_CREDENTIALS;
          }

          @Override
          public String safeDetail(
              InvalidCredentialsException exception, ServerWebExchange exchange) {
            return "y";
          }
        };
    List<ApiExceptionMapper<?>> input = List.of(m1, m2);
    assertThatThrownBy(() -> new ApiExceptionMapperRegistry(input))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Duplicate ApiExceptionMapper for type");
  }
}
