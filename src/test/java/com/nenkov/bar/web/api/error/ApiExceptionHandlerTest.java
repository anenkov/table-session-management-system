package com.nenkov.bar.web.api.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.nenkov.bar.web.api.error.factory.ApiProblemFactory;
import com.nenkov.bar.web.api.error.handler.ApiExceptionHandler;
import com.nenkov.bar.web.api.error.mapping.ApiExceptionMapperRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

class ApiExceptionHandlerTest {

  private final ApiExceptionHandler handler =
      new ApiExceptionHandler(new ApiProblemFactory(), new ApiExceptionMapperRegistry(List.of()));

  @Test
  void handleException_allowsNullExchange_forUnexpectedExceptions() {
    ResponseEntity<ProblemDetail> response =
        handler.handleException(new RuntimeException("boom"), null);

    assertThat(response.getStatusCode().value()).isEqualTo(500);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getProperties()).doesNotContainKey("correlationId");
  }
}
