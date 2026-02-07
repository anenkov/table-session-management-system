package com.nenkov.bar.application.session.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nenkov.bar.application.session.exception.TableSessionNotFoundException;
import com.nenkov.bar.application.session.model.CloseTableSessionInput;
import com.nenkov.bar.application.session.model.CloseTableSessionResult;
import com.nenkov.bar.application.session.repository.TableSessionRepository;
import com.nenkov.bar.domain.exceptions.IllegalDomainStateException;
import com.nenkov.bar.domain.model.session.TableSession;
import com.nenkov.bar.domain.model.session.TableSessionContents;
import com.nenkov.bar.domain.model.session.TableSessionId;
import com.nenkov.bar.domain.model.session.TableSessionStatus;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
final class CloseTableSessionHandlerTest {

  @Mock private TableSessionRepository tableSessionRepository;

  @Captor private ArgumentCaptor<TableSession> savedSessionCaptor;

  @Test
  void handle_whenFound_closesSession_savesClosedAggregate_andReturnsResult() {
    CloseTableSessionHandler handler = new CloseTableSessionHandler(tableSessionRepository);

    TableSessionId id = TableSessionId.of("S-1");
    TableSession open =
        new TableSession(id, "EUR", TableSessionContents.empty(), TableSessionStatus.OPEN, null);

    when(tableSessionRepository.findById(id)).thenReturn(Optional.of(open));

    CloseTableSessionResult result = handler.handle(new CloseTableSessionInput(id));

    // Save closed aggregate
    verify(tableSessionRepository).save(savedSessionCaptor.capture());
    TableSession saved = savedSessionCaptor.getValue();

    assertThat(saved.id()).isEqualTo(id);
    assertThat(saved.status()).isEqualTo(TableSessionStatus.CLOSED);
    assertThat(saved.closedAt()).isNotNull();

    // Result should reflect the saved aggregate
    assertThat(result.sessionId()).isEqualTo(id);
    assertThat(result.status()).isEqualTo(TableSessionStatus.CLOSED);
    assertThat(result.closedAt()).isEqualTo(saved.closedAt());

    // sanity: closedAt should be "now-ish" (not in the future)
    assertThat(result.closedAt()).isBeforeOrEqualTo(Instant.now());
  }

  @Test
  void handle_whenSessionMissing_throwsNotFound_andDoesNotSave() {
    CloseTableSessionHandler handler = new CloseTableSessionHandler(tableSessionRepository);

    TableSessionId id = TableSessionId.of("missing");
    CloseTableSessionInput input = new CloseTableSessionInput(id);

    when(tableSessionRepository.findById(id)).thenReturn(Optional.empty());

    Throwable thrown =
        assertThrows(TableSessionNotFoundException.class, () -> handler.handle(input));

    assertThat(thrown.getMessage()).contains("TableSession not found: " + id.value());
    verify(tableSessionRepository, never()).save(ArgumentMatchers.any());
  }

  @Test
  void handle_whenAlreadyClosed_propagatesDomainRejection_andDoesNotSave() {
    CloseTableSessionHandler handler = new CloseTableSessionHandler(tableSessionRepository);

    TableSessionId id = TableSessionId.of("S-closed");
    TableSession alreadyClosed =
        new TableSession(
            id,
            "EUR",
            TableSessionContents.empty(),
            TableSessionStatus.CLOSED,
            Instant.parse("2026-01-01T00:00:00Z"));
    CloseTableSessionInput input = new CloseTableSessionInput(id);

    when(tableSessionRepository.findById(id)).thenReturn(Optional.of(alreadyClosed));

    Throwable thrown = assertThrows(IllegalDomainStateException.class, () -> handler.handle(input));

    assertThat(thrown.getMessage()).contains("already CLOSED");
    verify(tableSessionRepository, never()).save(ArgumentMatchers.any());
  }

  @Test
  void handle_nullInput_throwsNpe() {
    CloseTableSessionHandler handler = new CloseTableSessionHandler(tableSessionRepository);

    Throwable thrown = assertThrows(NullPointerException.class, () -> handler.handle(null));

    assertThat(thrown.getMessage()).contains("input must not be null");
  }

  @Test
  void constructor_nullRepository_throwsNpe() {
    Throwable thrown =
        assertThrows(NullPointerException.class, () -> new CloseTableSessionHandler(null));

    assertThat(thrown.getMessage()).contains("tableSessionRepository must not be null");
  }
}
