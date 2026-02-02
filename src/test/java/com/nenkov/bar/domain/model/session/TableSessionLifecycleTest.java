package com.nenkov.bar.domain.model.session;

import static org.assertj.core.api.Assertions.assertThat;

import com.nenkov.bar.domain.exceptions.IllegalDomainStateException;
import com.nenkov.bar.domain.model.writeoff.ItemWriteOff;
import com.nenkov.bar.domain.model.writeoff.WriteOff;
import com.nenkov.bar.domain.service.payment.SessionItemSnapshot;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

final class TableSessionLifecycleTest {

  @Test
  void constructor_open_requiresClosedAtNull() {
    TableSessionId id = TableSessionId.of("S-1");
    List<SessionItemSnapshot> snapshots = List.of();
    List<ItemWriteOff> itemWriteOffs = List.of();
    List<WriteOff> writeOffs = List.of();
    Instant closedAt = Instant.parse("2026-01-01T00:00:00Z");

    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () ->
                new TableSession(
                    id,
                    "EUR",
                    snapshots,
                    itemWriteOffs,
                    writeOffs,
                    TableSessionStatus.OPEN,
                    closedAt));

    assertThat(thrown.getMessage()).contains("closedAt must be null when status is OPEN");
  }

  @Test
  void constructor_closed_requiresClosedAtNonNull() {
    TableSessionId id = TableSessionId.of("S-1");

    List<SessionItemSnapshot> snapshots = List.of();
    List<ItemWriteOff> itemWriteOffs = List.of();
    List<WriteOff> writeOffs = List.of();

    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () ->
                new TableSession(
                    id,
                    "EUR",
                    snapshots,
                    itemWriteOffs,
                    writeOffs,
                    TableSessionStatus.CLOSED,
                    null));

    assertThat(thrown.getMessage()).contains("closedAt must not be null when status is CLOSED");
  }

  @Test
  void closeByManager_transitionsToClosedWithTimestamp() {
    TableSessionId id = TableSessionId.of("S-1");
    TableSession open =
        new TableSession(id, "EUR", List.of(), List.of(), List.of(), TableSessionStatus.OPEN, null);

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
        new TableSession(id, "EUR", List.of(), List.of(), List.of(), TableSessionStatus.OPEN, null);

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
            List.of(),
            List.of(),
            List.of(),
            TableSessionStatus.CLOSED,
            Instant.parse("2026-01-01T00:00:00Z"));

    Instant closedAt = Instant.parse("2026-01-02T00:00:00Z");

    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalDomainStateException.class, () -> closed.closeByManager(closedAt));

    assertThat(thrown.getMessage()).contains("already CLOSED");
  }
}
