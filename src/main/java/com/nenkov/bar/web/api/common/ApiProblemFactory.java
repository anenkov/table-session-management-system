package com.nenkov.bar.web.api.common;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

/**
 * Factory for creating RFC7807 {@link ProblemDetail} instances following the API conventions.
 *
 * <p>Conventions:
 *
 * <ul>
 *   <li>{@code type} is a stable URN from {@link ApiProblemCode#typeUri()}.
 *   <li>Extension properties:
 *       <ul>
 *         <li>{@code code} – stable {@link ApiProblemCode} name
 *         <li>{@code timestamp} – ISO-8601 timestamp of error creation
 *         <li>{@code correlationId} – propagated from {@code X-Request-Id} header when present
 *         <li>{@code errors} – validation error details (validation failures only)
 *       </ul>
 * </ul>
 */
@Component
public class ApiProblemFactory {

  static final String PROP_CODE = "code";
  static final String PROP_TIMESTAMP = "timestamp";
  static final String PROP_CORRELATION_ID = "correlationId";
  static final String PROP_ERRORS = "errors";
  static final String HEADER_REQUEST_ID = "X-Request-Id";

  public ProblemDetail problem(ApiProblemCode code, String detail, ServerWebExchange exchange) {
    ProblemDetail problem = ProblemDetail.forStatus(code.status());
    problem.setTitle(code.title());
    problem.setType(code.typeUri());
    problem.setDetail(detail);

    applyStandardProperties(problem, code, exchange);
    return problem;
  }

  public ProblemDetail validation(
      ApiProblemCode code,
      String detail,
      List<FieldViolationDetail> errors,
      ServerWebExchange exchange) {

    ProblemDetail problem = problem(code, detail, exchange);
    problem.setProperty(PROP_ERRORS, errors);
    return problem;
  }

  private void applyStandardProperties(
      ProblemDetail problem, ApiProblemCode code, ServerWebExchange exchange) {

    problem.setProperty(PROP_CODE, code.name());
    problem.setProperty(PROP_TIMESTAMP, Instant.now().toString());
    correlationId(exchange).ifPresent(id -> problem.setProperty(PROP_CORRELATION_ID, id));
  }

  private static Optional<String> correlationId(ServerWebExchange exchange) {
    if (exchange == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(exchange.getRequest().getHeaders().getFirst(HEADER_REQUEST_ID))
        .filter(s -> !s.isBlank());
  }
}
