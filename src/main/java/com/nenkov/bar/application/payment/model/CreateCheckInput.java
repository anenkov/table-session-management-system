package com.nenkov.bar.application.payment.model;

import com.nenkov.bar.domain.model.payment.PaymentSelection;
import com.nenkov.bar.domain.model.session.TableSessionId;
import java.util.List;
import java.util.Objects;

/**
 * Input model for creating a check from a payer selection.
 *
 * <p>D3=3A: uses domain {@link PaymentSelection} directly to avoid mapping logic in the
 * application.
 */
public record CreateCheckInput(TableSessionId sessionId, List<PaymentSelection> selections) {

  public CreateCheckInput {
    Objects.requireNonNull(sessionId, "sessionId must not be null");
    Objects.requireNonNull(selections, "selections must not be null");
    if (selections.isEmpty()) {
      throw new IllegalArgumentException("selections must not be empty");
    }
  }
}
