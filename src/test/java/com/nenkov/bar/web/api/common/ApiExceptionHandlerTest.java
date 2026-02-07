package com.nenkov.bar.web.api.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

class ApiExceptionHandlerTest {

  private final ApiExceptionHandler handler = new ApiExceptionHandler();

  @Test
  void handleUnexpected_allowsNullExchange() {
    ResponseEntity<ProblemDetail> response =
        handler.handleUnexpected(new RuntimeException("boom"), null);

    assertThat(response.getStatusCode().value()).isEqualTo(500);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getProperties()).doesNotContainKey("correlationId");
  }
}
