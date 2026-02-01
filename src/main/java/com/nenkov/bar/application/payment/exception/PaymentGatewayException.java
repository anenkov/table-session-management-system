package com.nenkov.bar.application.payment.exception;

/**
 * Signals a technical failure when interacting with the external payment system.
 *
 * <p>This exception is not used for business outcomes such as declines.
 */
public final class PaymentGatewayException extends RuntimeException {

  public PaymentGatewayException(String message, Throwable cause) {
    super(message, cause);
  }

  public PaymentGatewayException(String message) {
    super(message);
  }
}
