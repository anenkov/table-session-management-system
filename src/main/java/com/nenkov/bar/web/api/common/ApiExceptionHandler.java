package com.nenkov.bar.web.api.common;

import com.nenkov.bar.auth.InvalidCredentialsException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

/**
 * Global WebFlux exception handling producing RFC7807 Problem Details (application/problem+json).
 *
 * <p>Conventions:
 *
 * <ul>
 *   <li>ProblemDetail#type is a URN (stable)
 *   <li>Extension properties: code, timestamp, correlationId, errors (for validation)
 * </ul>
 */
@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  private static final String PROP_CODE = "code";
  private static final String PROP_TIMESTAMP = "timestamp";
  private static final String PROP_CORRELATION_ID = "correlationId";
  private static final String PROP_ERRORS = "errors";
  private static final String HEADER_REQUEST_ID = "X-Request-Id";

  @ExceptionHandler(WebExchangeBindException.class)
  public ResponseEntity<ProblemDetail> handleValidation(
      WebExchangeBindException ex, ServerWebExchange exchange) {
    ApiProblemCode code = ApiProblemCode.VALIDATION_FAILED;

    ProblemDetail problem = ProblemDetail.forStatus(code.status());
    problem.setTitle(code.title());
    problem.setType(code.typeUri());
    problem.setDetail("Request validation failed.");

    List<FieldViolationDetail> errors =
        ex.getFieldErrors().stream().map(ApiExceptionHandler::toFieldViolation).toList();

    problem.setProperty(PROP_CODE, code.name());
    problem.setProperty(PROP_TIMESTAMP, Instant.now().toString());
    correlationId(exchange).ifPresent(id -> problem.setProperty(PROP_CORRELATION_ID, id));
    problem.setProperty(PROP_ERRORS, errors);

    return ResponseEntity.status(code.status()).body(problem);
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<ProblemDetail> handleInvalidCredentials(
      InvalidCredentialsException ignored, ServerWebExchange exchange) {

    ApiProblemCode code = ApiProblemCode.AUTH_INVALID_CREDENTIALS;

    ProblemDetail problem = ProblemDetail.forStatus(code.status());
    problem.setTitle(code.title());
    problem.setType(code.typeUri());
    problem.setDetail("Invalid username or password.");

    problem.setProperty(PROP_CODE, code.name());
    problem.setProperty(PROP_TIMESTAMP, Instant.now().toString());
    correlationId(exchange).ifPresent(id -> problem.setProperty(PROP_CORRELATION_ID, id));

    return ResponseEntity.status(code.status()).body(problem);
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ProblemDetail> handleResponseStatus(
      ResponseStatusException ex, ServerWebExchange exchange) {
    HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());

    ProblemDetail problem = ProblemDetail.forStatus(status);
    problem.setTitle(status.getReasonPhrase());
    problem.setType(ApiProblemCode.RESPONSE_STATUS.typeUri());
    problem.setDetail(Optional.ofNullable(ex.getReason()).orElse(status.getReasonPhrase()));

    problem.setProperty(PROP_CODE, "HTTP_" + status.value());
    problem.setProperty(PROP_TIMESTAMP, Instant.now().toString());
    correlationId(exchange).ifPresent(id -> problem.setProperty(PROP_CORRELATION_ID, id));

    return ResponseEntity.status(status).body(problem);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, ServerWebExchange exchange) {
    ApiProblemCode code = ApiProblemCode.INTERNAL_ERROR;

    // Log full details server-side; return safe message client-side.
    log.error("Unhandled exception", ex);

    ProblemDetail problem = ProblemDetail.forStatus(code.status());
    problem.setTitle(code.title());
    problem.setType(code.typeUri());
    problem.setDetail("An unexpected error occurred.");

    problem.setProperty(PROP_CODE, code.name());
    problem.setProperty(PROP_TIMESTAMP, Instant.now().toString());
    correlationId(exchange).ifPresent(id -> problem.setProperty(PROP_CORRELATION_ID, id));

    return ResponseEntity.status(code.status()).body(problem);
  }

  private static FieldViolationDetail toFieldViolation(FieldError fe) {
    String field = Objects.toString(fe.getField(), "");
    String issue = Objects.toString(fe.getDefaultMessage(), "Invalid value");
    return new FieldViolationDetail(field, issue);
  }

  private static Optional<String> correlationId(ServerWebExchange exchange) {
    if (exchange == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(exchange.getRequest().getHeaders().getFirst(HEADER_REQUEST_ID))
        .filter(s -> !s.isBlank());
  }
}
