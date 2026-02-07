package com.nenkov.bar.domain.model.session;

import static org.assertj.core.api.Assertions.assertThat;

import com.nenkov.bar.domain.exceptions.IllegalDomainStateException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

final class TableSessionLifecycleTest {

  @Test
  void constructor_open_requiresClosedAtNull() {
    TableSessionId id = TableSessionId.of("S-1");
    Instant closedAt = Instant.parse("2026-01-01T00:00:00Z");
    TableSessionContents contents = TableSessionContents.empty();

    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new TableSession(id, "EUR", contents, TableSessionStatus.OPEN, closedAt));

    assertThat(thrown.getMessage()).contains("closedAt must be null when status is OPEN");
  }

  @Test
  void constructor_closed_requiresClosedAtNonNull() {
    TableSessionId id = TableSessionId.of("S-1");
    TableSessionContents contents = TableSessionContents.empty();

    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new TableSession(id, "EUR", contents, TableSessionStatus.CLOSED, null));

    assertThat(thrown.getMessage()).contains("closedAt must not be null when status is CLOSED");
  }

  @Test
  void closeByManager_transitionsToClosedWithTimestamp() {
    TableSessionId id = TableSessionId.of("S-1");
    TableSession open =
        new TableSession(id, "EUR", TableSessionContents.empty(), TableSessionStatus.OPEN, null);

    Instant closedAt = Instant.parse("2026-01-01T00:00:00Z");
    TableSession closed = open.closeByManager(closedAt);

    assertThat(closed.status()).isEqualTo(TableSessionStatus.CLOSED);
    assertThat(closed.closedAt()).isEqualTo(closedAt);
    assertThat(closed.id()).isEqualTo(id);
    assertThat(closed.currency()).isEqualTo("EUR");
  }

  @Test
  void closeByManager_nullClosedAt_throwsNpe() {
    TableSessionId id = TableSessionId.of("S-1");
    TableSession open =
        new TableSession(id, "EUR", TableSessionContents.empty(), TableSessionStatus.OPEN, null);

    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class, () -> open.closeByManager(null));

    assertThat(thrown.getMessage()).contains("closedAt must not be null");
  }

  @Test
  void closeByManager_whenAlreadyClosed_throwsIllegalDomainState() {
    TableSessionId id = TableSessionId.of("S-1");
    TableSession closed =
        new TableSession(
            id,
            "EUR",
            TableSessionContents.empty(),
            TableSessionStatus.CLOSED,
            Instant.parse("2026-01-01T00:00:00Z"));

    Instant closedAt = Instant.parse("2026-01-02T00:00:00Z");

    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalDomainStateException.class, () -> closed.closeByManager(closedAt));

    assertThat(thrown.getMessage()).contains("already CLOSED");
  }
}
