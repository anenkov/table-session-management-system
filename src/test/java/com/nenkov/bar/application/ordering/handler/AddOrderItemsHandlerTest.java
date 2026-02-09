package com.nenkov.bar.application.ordering.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nenkov.bar.application.ordering.model.AddOrderItemsInput;
import com.nenkov.bar.application.ordering.model.AddOrderItemsResult;
import com.nenkov.bar.application.session.exception.TableSessionNotFoundException;
import com.nenkov.bar.application.session.repository.TableSessionRepository;
import com.nenkov.bar.domain.exceptions.OrderingNotAllowedException;
import com.nenkov.bar.domain.model.session.TableSession;
import com.nenkov.bar.domain.model.session.TableSessionContents;
import com.nenkov.bar.domain.model.session.TableSessionId;
import com.nenkov.bar.domain.model.session.TableSessionStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

final class AddOrderItemsHandlerTest {

  @Test
  void handle_nullInput_throwsNpe() {
    TableSessionRepository repository = Mockito.mock(TableSessionRepository.class);
    AddOrderItemsHandler handler = new AddOrderItemsHandler(repository);

    Throwable thrown = assertThrows(NullPointerException.class, () -> handler.handle(null));

    assertThat(thrown.getMessage()).contains("input must not be null");
  }

  @Test
  void handle_sessionNotFound_throwsTableSessionNotFoundException() {
    TableSessionRepository repository = Mockito.mock(TableSessionRepository.class);
    AddOrderItemsHandler handler = new AddOrderItemsHandler(repository);

    TableSessionId sessionId = TableSessionId.of("S-404");
    when(repository.findById(sessionId)).thenReturn(Optional.empty());

    AddOrderItemsInput input =
        new AddOrderItemsInput("S-404", List.of(new AddOrderItemsInput.RequestedItem("P-1", 1)));

    assertThrows(TableSessionNotFoundException.class, () -> handler.handle(input));
  }

  @Test
  void handle_addsItems_savesUpdatedSession_andReturnsCreatedIdsAsStrings() {
    TableSessionRepository repository = Mockito.mock(TableSessionRepository.class);
    AddOrderItemsHandler handler = new AddOrderItemsHandler(repository);

    TableSessionId sessionId = TableSessionId.of("S-1");
    TableSession session =
        new TableSession(
            sessionId, "EUR", TableSessionContents.empty(), TableSessionStatus.OPEN, null);

    when(repository.findById(sessionId)).thenReturn(Optional.of(session));

    AddOrderItemsInput input =
        new AddOrderItemsInput(
            "S-1",
            List.of(
                new AddOrderItemsInput.RequestedItem("P-1", 2),
                new AddOrderItemsInput.RequestedItem("P-2", 1)));

    AddOrderItemsResult result = handler.handle(input);

    assertThat(result.sessionId()).isEqualTo("S-1");
    assertThat(result.createdItemIds()).hasSize(2);
    assertThat(result.createdItemIds()).allMatch(id -> !id.isBlank());

    ArgumentCaptor<TableSession> savedCaptor = ArgumentCaptor.forClass(TableSession.class);
    verify(repository).save(savedCaptor.capture());

    TableSession saved = savedCaptor.getValue();
    assertThat(saved.orderItems()).hasSize(2);

    List<String> savedIds =
        saved.orderItems().stream().map(oi -> oi.id().value().toString()).toList();

    assertThat(result.createdItemIds()).containsExactlyInAnyOrderElementsOf(savedIds);
  }

  @Test
  void handle_closedSession_throws_and_doesNotSave() {
    TableSessionRepository repository = Mockito.mock(TableSessionRepository.class);
    AddOrderItemsHandler handler = new AddOrderItemsHandler(repository);

    TableSessionId sessionId = TableSessionId.of("S-closed");
    TableSession session =
        new TableSession(
            sessionId,
            "EUR",
            TableSessionContents.empty(),
            TableSessionStatus.CLOSED,
            Instant.parse("2026-02-09T10:00:00Z"));

    when(repository.findById(sessionId)).thenReturn(Optional.of(session));

    AddOrderItemsInput input =
        new AddOrderItemsInput("S-closed", List.of(new AddOrderItemsInput.RequestedItem("P-1", 1)));

    assertThrows(OrderingNotAllowedException.class, () -> handler.handle(input));

    verify(repository, never()).save(Mockito.any());
  }
}
