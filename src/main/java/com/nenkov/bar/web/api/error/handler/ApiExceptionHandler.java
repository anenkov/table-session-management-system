package com.nenkov.bar.web.api.error.handler;

import com.nenkov.bar.web.api.error.factory.ApiProblemFactory;
import com.nenkov.bar.web.api.error.mapping.ApiExceptionMapper;
import com.nenkov.bar.web.api.error.mapping.ApiExceptionMapperRegistry;
import com.nenkov.bar.web.api.error.model.ApiProblemCode;
import com.nenkov.bar.web.api.error.model.FieldViolationDetail;
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
 * Global WebFlux exception handler producing RFC 7807 {@code ProblemDetail} responses ({@code
 * application/problem+json}).
 *
 * <p>Centralized mapping layer between exceptions, HTTP semantics, and {@link ApiProblemCode}.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  private final ApiProblemFactory problemFactory;
  private final ApiExceptionMapperRegistry mapperRegistry;

  public ApiExceptionHandler(
      ApiProblemFactory problemFactory, ApiExceptionMapperRegistry mapperRegistry) {
    this.problemFactory = Objects.requireNonNull(problemFactory, "problemFactory must not be null");
    this.mapperRegistry = Objects.requireNonNull(mapperRegistry, "mapperRegistry must not be null");
  }

  @ExceptionHandler(WebExchangeBindException.class)
  public ResponseEntity<ProblemDetail> handleValidation(
      WebExchangeBindException ex, ServerWebExchange exchange) {

    ApiProblemCode code = ApiProblemCode.VALIDATION_FAILED;

    List<FieldViolationDetail> errors =
        ex.getFieldErrors().stream().map(ApiExceptionHandler::toFieldViolation).toList();

    ProblemDetail problem =
        problemFactory.validation(code, "Request validation failed.", errors, exchange);

    return ResponseEntity.status(code.status()).body(problem);
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ProblemDetail> handleResponseStatus(
      ResponseStatusException ex, ServerWebExchange exchange) {
    Optional<ApiExceptionMapper<ResponseStatusException>> mapperOpt = mapperRegistry.findExact(ex);
    if (mapperOpt.isPresent()) {
      ApiExceptionMapper<ResponseStatusException> mapper = mapperOpt.get();
      ApiProblemCode code = mapper.code();
      String detail = mapper.safeDetail(ex, exchange);
      ProblemDetail problem = problemFactory.problem(code, detail, exchange);
      return ResponseEntity.status(code.status()).body(problem);
    }

    HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());

    ProblemDetail problem = ProblemDetail.forStatus(status);
    problem.setTitle(status.getReasonPhrase());
    problem.setType(ApiProblemCode.RESPONSE_STATUS.typeUri());
    problem.setDetail(resolveResponseStatusDetail(ex, status));

    // Convention: generic HTTP_xxx code for ResponseStatusException
    problem.setProperty(ApiProblemFactory.PROP_CODE, "HTTP_" + status.value());
    problem.setProperty(ApiProblemFactory.PROP_TIMESTAMP, Instant.now().toString());
    Optional.ofNullable(exchange)
        .map(e -> e.getRequest().getHeaders().getFirst(ApiProblemFactory.HEADER_REQUEST_ID))
        .filter(s -> !s.isBlank())
        .ifPresent(id -> problem.setProperty(ApiProblemFactory.PROP_CORRELATION_ID, id));

    return ResponseEntity.status(status).body(problem);
  }

  private static String resolveResponseStatusDetail(ResponseStatusException ex, HttpStatus status) {
    String reason = ex.getReason();
    if (reason != null && !reason.isBlank() && !"Type mismatch.".equals(reason)) {
      return reason;
    }

    return findNestedResponseStatusReason(ex)
        .orElse(
            Optional.ofNullable(reason).filter(s -> !s.isBlank()).orElse(status.getReasonPhrase()));
  }

  private static Optional<String> findNestedResponseStatusReason(Throwable throwable) {
    Throwable current = throwable == null ? null : throwable.getCause();
    int depth = 0;
    while (current != null && depth < 10) {
      if (current instanceof ResponseStatusException nested) {
        String reason = nested.getReason();
        if (reason != null && !reason.isBlank()) {
          return Optional.of(reason);
        }
      }
      current = current.getCause();
      depth++;
    }
    return Optional.empty();
  }

  /**
   * Dispatcher for all other exceptions.
   *
   * <p>Resolution is deterministic: exact exception class must have a registered {@link
   * ApiExceptionMapper}. Unknown exceptions become {@code INTERNAL_ERROR}.
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleException(Exception ex, ServerWebExchange exchange) {
    return handleMappedOrFallback(ex, exchange);
  }

  private ResponseEntity<ProblemDetail> handleMappedOrFallback(
      Exception ex, ServerWebExchange exchange) {

    Optional<ApiExceptionMapper<Exception>> mapperOpt = mapperRegistry.findExact(ex);
    if (mapperOpt.isPresent()) {
      return handleMapped(ex, exchange, mapperOpt.get());
    }

    log.error("Unhandled exception", ex);
    ApiProblemCode code = ApiProblemCode.INTERNAL_ERROR;

    ProblemDetail problem = problemFactory.problem(code, "An unexpected error occurred.", exchange);
    return ResponseEntity.status(code.status()).body(problem);
  }

  private ResponseEntity<ProblemDetail> handleMapped(
      Exception ex, ServerWebExchange exchange, ApiExceptionMapper<Exception> mapper) {

    ApiProblemCode code = mapper.code();
    String detail = mapper.safeDetail(ex, exchange);

    ProblemDetail problem = problemFactory.problem(code, detail, exchange);
    return ResponseEntity.status(code.status()).body(problem);
  }

  /** Converts a Spring {@link FieldError} into a stable API validation error detail. */
  private static FieldViolationDetail toFieldViolation(FieldError fe) {
    String field = Objects.toString(fe.getField(), "");
    String issue = Objects.toString(fe.getDefaultMessage(), "Invalid value");
    return new FieldViolationDetail(field, issue);
  }
}
