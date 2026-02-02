package com.nenkov.bar.domain.model.payment;

import static com.nenkov.bar.testsupport.TestFixtures.money;
import static org.junit.jupiter.api.Assertions.*;

import com.nenkov.bar.domain.exceptions.IllegalDomainStateException;
import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.session.OrderItemId;
import com.nenkov.bar.domain.model.session.TableSessionId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CheckTest {

  @Test
  void create_success_setsSessionId_setsCreatedStatus_andCopiesPaidItems() {
    TableSessionId sessionId = TableSessionId.of("session-1");
    CheckId id = CheckId.of(UUID.randomUUID());
    Instant createdAt = Instant.parse("2026-01-31T10:00:00Z");

    PaidItem item =
        PaidItem.of(
            OrderItemId.of(UUID.randomUUID()), 1, money("USD", "5.00"), money("USD", "5.00"));

    List<PaidItem> original = new ArrayList<>();
    original.add(item);

    Check check = Check.create(sessionId, id, money("USD", "5.00"), original, createdAt);

    assertEquals(sessionId, check.sessionId());
    assertEquals(id, check.id());
    assertEquals(CheckStatus.CREATED, check.status());
    assertEquals(createdAt, check.createdAt());
    assertNull(check.paymentReference());
    assertNull(check.completedAt());

    // Defensive copy: mutating the input list after creation must not affect the check.
    original.clear();
    assertEquals(1, check.paidItems().size());
    assertEquals(item, check.paidItems().getFirst());
  }

  @Test
  void sessionId_mustNotBeNull() {
    Instant createdAt = Instant.parse("2026-01-31T10:00:00Z");

    PaidItem item =
        PaidItem.of(
            OrderItemId.of(UUID.randomUUID()), 1, money("USD", "5.00"), money("USD", "5.00"));

    Money amount = money("USD", "5.00");
    List<PaidItem> items = List.of(item);

    NullPointerException ex =
        assertThrows(
            NullPointerException.class, () -> Check.createNew(null, amount, items, createdAt));

    assertTrue(ex.getMessage().toLowerCase().contains("sessionid"));
  }

  @Test
  void create_requiresSessionId() {
    CheckId id = CheckId.of(UUID.randomUUID());
    Instant createdAt = Instant.parse("2026-01-31T10:00:00Z");

    PaidItem item =
        PaidItem.of(
            OrderItemId.of(UUID.randomUUID()), 1, money("USD", "5.00"), money("USD", "5.00"));
    Money amount = money("USD", "5.00");
    List<PaidItem> items = List.of(item);

    assertThrows(
        NullPointerException.class, () -> Check.create(null, id, amount, items, createdAt));
  }

  @Test
  void amount_mustBeGreaterThanZero() {
    TableSessionId sessionId = TableSessionId.of("session-1");
    Instant createdAt = Instant.parse("2026-01-31T10:00:00Z");

    PaidItem item =
        PaidItem.of(
            OrderItemId.of(UUID.randomUUID()), 1, money("USD", "5.00"), money("USD", "5.00"));

    Money zeroMoney = Money.zero("USD");
    List<PaidItem> items = List.of(item);

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> Check.createNew(sessionId, zeroMoney, items, createdAt));

    assertTrue(ex.getMessage().toLowerCase().contains("greater than zero"));
  }

  @Test
  void paidItems_mustNotBeEmpty() {
    TableSessionId sessionId = TableSessionId.of("session-1");
    Instant createdAt = Instant.parse("2026-01-31T10:00:00Z");
    Money amount = money("USD", "1.00");
    List<PaidItem> items = List.of();

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> Check.createNew(sessionId, amount, items, createdAt));

    assertTrue(ex.getMessage().toLowerCase().contains("paiditems"));
  }

  @Test
  void currency_mismatch_betweenCheckAndItems_rejected() {
    TableSessionId sessionId = TableSessionId.of("session-1");
    Instant createdAt = Instant.parse("2026-01-31T10:00:00Z");

    PaidItem item =
        PaidItem.of(
            OrderItemId.of(UUID.randomUUID()), 1, money("EUR", "5.00"), money("EUR", "5.00"));

    Money amount = money("USD", "5.00");
    List<PaidItem> items = List.of(item);

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> Check.createNew(sessionId, amount, items, createdAt));

    assertTrue(ex.getMessage().toLowerCase().contains("currency mismatch"));
  }

  @Test
  void markPaid_fromCreated_succeeds_setsReference_status_andCompletedAt() {
    Instant createdAt = Instant.parse("2026-01-31T10:00:00Z");
    Instant completedAt = Instant.parse("2026-01-31T10:05:00Z");

    Check check = newCreatedCheck(createdAt);

    check.markPaid(PaymentReference.of("pay-1"), completedAt);

    assertEquals(CheckStatus.PAID, check.status());
    assertEquals("pay-1", check.paymentReference().value());
    assertEquals(completedAt, check.completedAt());
  }

  @Test
  void markAuthorized_thenMarkPaid_succeeds() {
    Instant createdAt = Instant.parse("2026-01-31T10:00:00Z");
    Instant completedAt = Instant.parse("2026-01-31T10:05:00Z");

    Check check = newCreatedCheck(createdAt);

    check.markAuthorized(PaymentReference.of("auth-1"));
    assertEquals(CheckStatus.AUTHORIZED, check.status());
    assertEquals("auth-1", check.paymentReference().value());

    check.markPaid(PaymentReference.of("cap-1"), completedAt);
    assertEquals(CheckStatus.PAID, check.status());
    assertEquals("cap-1", check.paymentReference().value());
    assertEquals(completedAt, check.completedAt());
  }

  @Test
  void terminal_states_reject_further_transitions() {
    Instant createdAt = Instant.parse("2026-01-31T10:00:00Z");
    Instant completedAt = Instant.parse("2026-01-31T10:05:00Z");

    Check check = newCreatedCheck(createdAt);
    check.markPaid(PaymentReference.of("pay-1"), completedAt);

    PaymentReference ref = PaymentReference.of("x");

    assertThrows(IllegalDomainStateException.class, () -> check.cancel(completedAt));
    assertThrows(IllegalDomainStateException.class, () -> check.markFailed(completedAt));
    assertThrows(IllegalDomainStateException.class, () -> check.markAuthorized(ref));
  }

  private static Check newCreatedCheck(Instant createdAt) {
    TableSessionId sessionId = TableSessionId.of("session-1");

    PaidItem item =
        PaidItem.of(
            OrderItemId.of(UUID.randomUUID()), 1, money("USD", "5.00"), money("USD", "5.00"));

    return Check.createNew(sessionId, money("USD", "5.00"), List.of(item), createdAt);
  }
}
