package com.nenkov.bar.application.payment.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.nenkov.bar.domain.model.payment.PaymentSelection;
import com.nenkov.bar.domain.model.session.OrderItemId;
import com.nenkov.bar.domain.model.session.TableSessionId;
import java.util.List;
import org.junit.jupiter.api.Test;

final class CreateCheckInputTest {

  @Test
  void happyPath() {
    TableSessionId sessionId = TableSessionId.of("S-1");
    PaymentSelection selection = PaymentSelection.of(OrderItemId.random(), 1);
    List<PaymentSelection> selections = List.of(selection);

    CreateCheckInput input = new CreateCheckInput(sessionId, selections);

    assertThat(input.sessionId()).isEqualTo(sessionId);
    assertThat(input.selections()).containsExactly(selection);
  }

  @Test
  void nullSessionId_throwsNpe() {
    PaymentSelection selection = PaymentSelection.of(OrderItemId.random(), 1);
    List<PaymentSelection> selections = List.of(selection);

    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class, () -> new CreateCheckInput(null, selections));

    assertThat(thrown.getMessage()).contains("sessionId must not be null");
  }

  @Test
  void nullSelections_throwsNpe() {
    TableSessionId sessionId = TableSessionId.of("S-1");

    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class, () -> new CreateCheckInput(sessionId, null));

    assertThat(thrown.getMessage()).contains("selections must not be null");
  }

  @Test
  void emptySelections_throwsIae() {
    TableSessionId sessionId = TableSessionId.of("S-1");
    List<PaymentSelection> selections = List.of();

    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class, () -> new CreateCheckInput(sessionId, selections));

    assertThat(thrown.getMessage()).contains("selections must not be empty");
  }
}
