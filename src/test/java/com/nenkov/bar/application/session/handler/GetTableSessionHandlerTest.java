package com.nenkov.bar.application.session.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.nenkov.bar.application.session.exception.TableSessionNotFoundException;
import com.nenkov.bar.application.session.model.GetTableSessionInput;
import com.nenkov.bar.application.session.model.GetTableSessionResult;
import com.nenkov.bar.application.session.repository.TableSessionRepository;
import com.nenkov.bar.domain.model.session.TableSession;
import com.nenkov.bar.domain.model.session.TableSessionContents;
import com.nenkov.bar.domain.model.session.TableSessionId;
import com.nenkov.bar.domain.model.session.TableSessionStatus;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
final class GetTableSessionHandlerTest {

  @Mock private TableSessionRepository tableSessionRepository;

  @Test
  void handle_whenFound_returnsReadProjection() {
    GetTableSessionHandler handler = new GetTableSessionHandler(tableSessionRepository);

    TableSessionId id = TableSessionId.of("S-1");
    TableSession session =
        new TableSession(id, "EUR", TableSessionContents.empty(), TableSessionStatus.OPEN, null);

    when(tableSessionRepository.findById(id)).thenReturn(Optional.of(session));

    GetTableSessionResult result = handler.handle(new GetTableSessionInput(id));

    assertThat(result.sessionId()).isEqualTo(id);
    assertThat(result.currency()).isEqualTo("EUR");
    assertThat(result.payableItems()).isSameAs(session.payableItemsSnapshot());
    assertThat(result.itemWriteOffs()).isSameAs(session.itemWriteOffs());
    assertThat(result.sessionWriteOffs()).isSameAs(session.sessionWriteOffs());
  }

  @Test
  void handle_whenMissing_throwsNotFound() {
    GetTableSessionHandler handler = new GetTableSessionHandler(tableSessionRepository);

    TableSessionId id = TableSessionId.of("missing");
    when(tableSessionRepository.findById(id)).thenReturn(Optional.empty());

    GetTableSessionInput input = new GetTableSessionInput(id);

    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            TableSessionNotFoundException.class, () -> handler.handle(input));

    assertThat(thrown.getMessage()).contains("TableSession not found: " + id.value());
  }

  @Test
  void handle_nullInput_throwsNpe() {
    GetTableSessionHandler handler = new GetTableSessionHandler(tableSessionRepository);

    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class, () -> handler.handle(null));

    assertThat(thrown.getMessage()).contains("input must not be null");
  }

  @Test
  void constructor_nullRepository_throwsNpe() {
    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class, () -> new GetTableSessionHandler(null));

    assertThat(thrown.getMessage()).contains("tableSessionRepository must not be null");
  }
}
