package com.nenkov.bar.web.api.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.nenkov.bar.web.api.error.factory.ApiProblemFactory;
import com.nenkov.bar.web.api.error.handler.ApiExceptionHandler;
import com.nenkov.bar.web.api.error.mapping.ApiExceptionMapperRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

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

  @Test
  void handleResponseStatus_usesNestedReason_whenOuterReasonIsTypeMismatch() {
    ResponseStatusException nested =
        new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sessionId.");
    ResponseStatusException wrapped =
        new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type mismatch.", nested);

    ResponseEntity<ProblemDetail> response = handler.handleResponseStatus(wrapped, null);

    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getDetail()).isEqualTo("Invalid sessionId.");
    assertThat(response.getBody().getProperties()).containsEntry("code", "HTTP_400");
  }
}
