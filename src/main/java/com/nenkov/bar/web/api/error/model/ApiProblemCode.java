package com.nenkov.bar.web.api.error.model;

import java.net.URI;
import org.springframework.http.HttpStatus;

/**
 * Stable, machine-readable API problem codes, mapped to RFC7807 Problem Details.
 *
 * <p>Note: {@link #typeUri()} uses URNs to avoid coupling to a hosted URL.
 */
public enum ApiProblemCode {
  VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Validation failed", "urn:problem:validation-failed"),
  SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "Session not found", "urn:problem:session-not-found"),
  SESSION_ALREADY_OPEN_FOR_TABLE(
      HttpStatus.CONFLICT,
      "Session already open for table",
      "urn:problem:session-already-open-for-table"),
  AUTH_INVALID_CREDENTIALS(
      HttpStatus.UNAUTHORIZED, "Invalid credentials", "urn:problem:auth-invalid-credentials"),
  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", "urn:problem:internal-error"),
  RESPONSE_STATUS(HttpStatus.BAD_REQUEST, "Request failed", "urn:problem:response-status"),
  ORDERING_CONFLICT(HttpStatus.CONFLICT, "Ordering conflict", "urn:problem:ordering-conflict"),
  PAYMENT_CONFLICT(HttpStatus.CONFLICT, "Payment conflict", "urn:problem:payment-conflict"),
  PAYMENT_REQUEST_CONFLICT(
      HttpStatus.CONFLICT, "Payment request conflict", "urn:problem:payment-request-conflict"),
  CHECK_NOT_FOUND(HttpStatus.NOT_FOUND, "Check not found", "urn:problem:check-not-found"),
  PAYMENT_SELECTION_INVALID(
      HttpStatus.BAD_REQUEST, "Invalid payment selection", "urn:problem:payment-selection-invalid");

  private final HttpStatus status;
  private final String title;
  private final URI typeUri;

  ApiProblemCode(HttpStatus status, String title, String type) {
    this.status = status;
    this.title = title;
    this.typeUri = URI.create(type);
  }

  public HttpStatus status() {
    return status;
  }

  public String title() {
    return title;
  }

  public URI typeUri() {
    return typeUri;
  }
}
