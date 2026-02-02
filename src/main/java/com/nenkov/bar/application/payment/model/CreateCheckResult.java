package com.nenkov.bar.application.payment.model;

import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.payment.CheckId;
import com.nenkov.bar.domain.model.session.TableSessionId;
import java.util.Objects;

/** Result model for creating a check. */
public record CreateCheckResult(TableSessionId sessionId, CheckId checkId, Money amount) {

  public CreateCheckResult {
    Objects.requireNonNull(sessionId, "sessionId must not be null");
    Objects.requireNonNull(checkId, "checkId must not be null");
    Objects.requireNonNull(amount, "amount must not be null");
  }
}
