package com.nenkov.bar.application.payment.exception;

import com.nenkov.bar.domain.model.payment.CheckId;

/** Thrown when a payment workflow requires a check that does not exist. */
public final class CheckNotFoundException extends RuntimeException {

  public CheckNotFoundException(CheckId checkId) {
    super("Check not found: " + checkId.value());
  }
}
