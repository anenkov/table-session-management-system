package com.nenkov.bar.application.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nenkov.bar.application.session.handler.CloseTableSessionHandler;
import com.nenkov.bar.application.session.handler.GetTableSessionHandler;
import com.nenkov.bar.application.session.handler.OpenTableSessionHandler;
import com.nenkov.bar.application.session.model.CloseTableSessionInput;
import com.nenkov.bar.application.session.model.CloseTableSessionResult;
import com.nenkov.bar.application.session.model.GetTableSessionInput;
import com.nenkov.bar.application.session.model.GetTableSessionResult;
import com.nenkov.bar.application.session.model.OpenTableSessionInput;
import com.nenkov.bar.application.session.model.OpenTableSessionResult;
import com.nenkov.bar.domain.model.session.TableSessionId;
import com.nenkov.bar.domain.model.session.TableSessionStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
final class DefaultTableSessionServiceTest {

  @Mock private OpenTableSessionHandler openTableSessionHandler;
  @Mock private GetTableSessionHandler getTableSessionHandler;
  @Mock private CloseTableSessionHandler closeTableSessionHandler;

  @Test
  void open_delegatesToHandler() {
    DefaultTableSessionService service =
        new DefaultTableSessionService(
            openTableSessionHandler, getTableSessionHandler, closeTableSessionHandler);

    OpenTableSessionInput input = new OpenTableSessionInput("T-1");
    OpenTableSessionResult expected = new OpenTableSessionResult("S-1", "T-1");
    when(openTableSessionHandler.handle(input)).thenReturn(expected);

    OpenTableSessionResult actual = service.open(input);

    assertThat(actual).isSameAs(expected);
    verify(openTableSessionHandler).handle(input);
  }

  @Test
  void getById_delegatesToHandler() {
    DefaultTableSessionService service =
        new DefaultTableSessionService(
            openTableSessionHandler, getTableSessionHandler, closeTableSessionHandler);

    TableSessionId id = TableSessionId.of("S-1");
    GetTableSessionInput input = new GetTableSessionInput(id);
    GetTableSessionResult expected =
        new GetTableSessionResult(id, "EUR", List.of(), List.of(), List.of());
    when(getTableSessionHandler.handle(input)).thenReturn(expected);

    GetTableSessionResult actual = service.getById(input);

    assertThat(actual).isSameAs(expected);
    verify(getTableSessionHandler).handle(input);
  }

  @Test
  void close_delegatesToHandler() {
    DefaultTableSessionService service =
        new DefaultTableSessionService(
            openTableSessionHandler, getTableSessionHandler, closeTableSessionHandler);

    TableSessionId id = TableSessionId.of("S-1");
    CloseTableSessionInput input = new CloseTableSessionInput(id);
    CloseTableSessionResult expected =
        new CloseTableSessionResult(
            id, TableSessionStatus.CLOSED, Instant.parse("2026-01-01T00:00:00Z"));
    when(closeTableSessionHandler.handle(input)).thenReturn(expected);

    CloseTableSessionResult actual = service.close(input);

    assertThat(actual).isSameAs(expected);
    verify(closeTableSessionHandler).handle(input);
  }

  @Test
  void constructor_nullOpenHandler_throwsNpe() {
    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class,
            () ->
                new DefaultTableSessionService(
                    null, getTableSessionHandler, closeTableSessionHandler));

    assertThat(thrown.getMessage()).contains("openTableSessionHandler must not be null");
  }

  @Test
  void constructor_nullGetHandler_throwsNpe() {
    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class,
            () ->
                new DefaultTableSessionService(
                    openTableSessionHandler, null, closeTableSessionHandler));

    assertThat(thrown.getMessage()).contains("getTableSessionHandler must not be null");
  }

  @Test
  void constructor_nullCloseHandler_throwsNpe() {
    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class,
            () ->
                new DefaultTableSessionService(
                    openTableSessionHandler, getTableSessionHandler, null));

    assertThat(thrown.getMessage()).contains("closeTableSessionHandler must not be null");
  }
}
