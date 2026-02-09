package com.nenkov.bar.web.api.common;

import com.nenkov.bar.application.session.exception.TableAlreadyHasOpenSessionException;
import com.nenkov.bar.application.session.exception.TableSessionNotFoundException;
import com.nenkov.bar.auth.InvalidCredentialsException;
import com.nenkov.bar.domain.exceptions.OrderingNotAllowedException;
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
 * <p>This class is the single, centralized mapping layer between:
 *
 * <ul>
 *   <li>Application and infrastructure exceptions
 *   <li>HTTP status codes
 *   <li>Stable API error identifiers ({@link ApiProblemCode})
 * </ul>
 *
 * <h3>Conventions</h3>
 *
 * <ul>
 *   <li>{@code ProblemDetail.type} is a stable URN
 *   <li>Extension properties:
 *       <ul>
 *         <li>{@code code} – stable {@link ApiProblemCode} name or HTTP code
 *         <li>{@code timestamp} – ISO-8601 timestamp of error creation
 *         <li>{@code correlationId} – propagated from {@code X-Request-Id} header when present
 *         <li>{@code errors} – validation error details (validation failures only)
 *       </ul>
 *   <li>Exception messages are not leaked directly to clients unless explicitly safe
 * </ul>
 *
 * <p>All handlers are deterministic and side-effect-free.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  private static final String PROP_CODE = "code";
  private static final String PROP_TIMESTAMP = "timestamp";
  private static final String PROP_CORRELATION_ID = "correlationId";
  private static final String PROP_ERRORS = "errors";
  private static final String HEADER_REQUEST_ID = "X-Request-Id";

  /**
   * Handles request validation failures triggered by WebFlux binding or Bean Validation.
   *
   * <p>Produces {@code 400 Bad Request} with a structured list of field-level violations.
   *
   * @param ex validation exception raised during request binding
   * @param exchange current server exchange
   * @return RFC7807 {@link ProblemDetail} response with validation errors
   */
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

  /**
   * Handles authentication failures caused by invalid user credentials.
   *
   * <p>Produces {@code 401 Unauthorized}. The response detail is intentionally generic to avoid
   * leaking authentication information.
   *
   * @param ignored invalid credentials exception
   * @param exchange current server exchange
   * @return RFC7807 {@link ProblemDetail} response
   */
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

  /**
   * Handles attempts to access a table session that does not exist.
   *
   * <p>Produces {@code 404 Not Found}. This exception originates from the application layer and is
   * part of the public API contract.
   *
   * @param ignored session-not-found exception
   * @param exchange current server exchange
   * @return RFC7807 {@link ProblemDetail} response
   */
  @ExceptionHandler(TableSessionNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleSessionNotFound(
      TableSessionNotFoundException ignored, ServerWebExchange exchange) {

    ApiProblemCode code = ApiProblemCode.SESSION_NOT_FOUND;

    ProblemDetail problem = ProblemDetail.forStatus(code.status());
    problem.setTitle(code.title());
    problem.setType(code.typeUri());
    problem.setDetail("Session was not found.");

    problem.setProperty(PROP_CODE, code.name());
    problem.setProperty(PROP_TIMESTAMP, Instant.now().toString());
    correlationId(exchange).ifPresent(id -> problem.setProperty(PROP_CORRELATION_ID, id));

    return ResponseEntity.status(code.status()).body(problem);
  }

  /**
   * Handles conflicts caused by attempting to open a new session for a table that already has an
   * active session.
   *
   * <p>Produces {@code 409 Conflict}. This enforces the one-open-session-per-table business rule at
   * the API boundary.
   *
   * @param ignored conflict exception
   * @param exchange current server exchange
   * @return RFC7807 {@link ProblemDetail} response
   */
  @ExceptionHandler(TableAlreadyHasOpenSessionException.class)
  public ResponseEntity<ProblemDetail> handleOpenSessionConflict(
      TableAlreadyHasOpenSessionException ignored, ServerWebExchange exchange) {

    ApiProblemCode code = ApiProblemCode.SESSION_ALREADY_OPEN_FOR_TABLE;

    ProblemDetail problem = ProblemDetail.forStatus(code.status());
    problem.setTitle(code.title());
    problem.setType(code.typeUri());
    problem.setDetail("An open session already exists for this table.");

    problem.setProperty(PROP_CODE, code.name());
    problem.setProperty(PROP_TIMESTAMP, Instant.now().toString());
    correlationId(exchange).ifPresent(id -> problem.setProperty(PROP_CORRELATION_ID, id));

    return ResponseEntity.status(code.status()).body(problem);
  }

  /**
   * Handles {@link ResponseStatusException} thrown explicitly by controllers or infrastructure
   * code.
   *
   * <p>The HTTP status is preserved and exposed using a generic {@code HTTP_xxx} problem code.
   *
   * @param ex response status exception
   * @param exchange current server exchange
   * @return RFC7807 {@link ProblemDetail} response
   */
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

  /**
   * Fallback handler for all uncaught exceptions.
   *
   * <p>Produces {@code 500 Internal Server Error}. Full details are logged server-side, while the
   * client receives a safe, non-specific error message.
   *
   * @param ex unexpected exception
   * @param exchange current server exchange
   * @return RFC7807 {@link ProblemDetail} response
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, ServerWebExchange exchange) {
    ApiProblemCode code = ApiProblemCode.INTERNAL_ERROR;

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

  /**
   * Handles ordering conflicts when the session state does not allow adding new order items.
   *
   * <p>Produces {@code 409 Conflict}. The response detail is intentionally generic and does not
   * leak internal exception messages.
   *
   * @param ignored ordering-not-allowed exception
   * @param exchange current server exchange
   * @return RFC7807 {@link ProblemDetail} response
   */
  @ExceptionHandler(OrderingNotAllowedException.class)
  public ResponseEntity<ProblemDetail> handleOrderingNotAllowed(
      OrderingNotAllowedException ignored, ServerWebExchange exchange) {

    ApiProblemCode code = ApiProblemCode.ORDERING_CONFLICT;

    ProblemDetail problem = ProblemDetail.forStatus(code.status());
    problem.setTitle(code.title());
    problem.setType(code.typeUri());
    problem.setDetail("Ordering is not allowed for the current session state.");

    problem.setProperty(PROP_CODE, code.name());
    problem.setProperty(PROP_TIMESTAMP, Instant.now().toString());
    correlationId(exchange).ifPresent(id -> problem.setProperty(PROP_CORRELATION_ID, id));

    return ResponseEntity.status(code.status()).body(problem);
  }

  /** Converts a Spring {@link FieldError} into a stable API validation error detail. */
  private static FieldViolationDetail toFieldViolation(FieldError fe) {
    String field = Objects.toString(fe.getField(), "");
    String issue = Objects.toString(fe.getDefaultMessage(), "Invalid value");
    return new FieldViolationDetail(field, issue);
  }

  /** Extracts correlation ID from {@code X-Request-Id} header if present. */
  private static Optional<String> correlationId(ServerWebExchange exchange) {
    if (exchange == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(exchange.getRequest().getHeaders().getFirst(HEADER_REQUEST_ID))
        .filter(s -> !s.isBlank());
  }
}
