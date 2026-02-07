package com.nenkov.bar.application.session.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nenkov.bar.application.common.config.ApplicationCurrency;
import com.nenkov.bar.application.session.exception.TableAlreadyHasOpenSessionException;
import com.nenkov.bar.application.session.model.OpenTableSessionInput;
import com.nenkov.bar.application.session.model.OpenTableSessionResult;
import com.nenkov.bar.application.session.repository.TableSessionRepository;
import com.nenkov.bar.domain.model.session.TableSession;
import com.nenkov.bar.domain.model.session.TableSessionStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
final class OpenTableSessionHandlerTest {

  @Mock private TableSessionRepository tableSessionRepository;

  @Captor private ArgumentCaptor<TableSession> sessionCaptor;

  @Test
  void handle_whenNoOpenSession_persistsOpenSession_andReturnsIds() {
    OpenTableSessionHandler handler =
        new OpenTableSessionHandler(tableSessionRepository, new ApplicationCurrency("EUR"));

    OpenTableSessionInput input = new OpenTableSessionInput("T-12");

    when(tableSessionRepository.existsOpenByTableId("T-12")).thenReturn(false);

    OpenTableSessionResult result = handler.handle(input);

    verify(tableSessionRepository).save(sessionCaptor.capture());
    TableSession saved = sessionCaptor.getValue();

    assertThat(saved).isNotNull();
    assertThat(saved.id()).isNotNull();
    assertThat(saved.id().value()).isNotBlank();

    assertThat(saved.currency()).isEqualTo("EUR");
    assertThat(saved.status()).isEqualTo(TableSessionStatus.OPEN);
    assertThat(saved.closedAt()).isNull();

    assertThat(saved.payableItemsSnapshot()).isEqualTo(List.of());
    assertThat(saved.itemWriteOffs()).isEqualTo(List.of());
    assertThat(saved.sessionWriteOffs()).isEqualTo(List.of());

    assertThat(result.sessionId()).isEqualTo(saved.id().value());
    assertThat(result.tableId()).isEqualTo("T-12");
  }

  @Test
  void handle_whenTableAlreadyHasOpenSession_throws_andDoesNotSave() {
    OpenTableSessionHandler handler =
        new OpenTableSessionHandler(tableSessionRepository, new ApplicationCurrency("EUR"));

    OpenTableSessionInput input = new OpenTableSessionInput("T-1");
    when(tableSessionRepository.existsOpenByTableId("T-1")).thenReturn(true);

    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            TableAlreadyHasOpenSessionException.class, () -> handler.handle(input));

    assertThat(thrown.getMessage()).contains("Table already has an open session: T-1");

    verify(tableSessionRepository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void handle_generatesNewSessionIdEveryTime_whenTableHasNoOpenSession() {
    OpenTableSessionHandler handler =
        new OpenTableSessionHandler(tableSessionRepository, new ApplicationCurrency("EUR"));

    when(tableSessionRepository.existsOpenByTableId("T-1")).thenReturn(false);

    handler.handle(new OpenTableSessionInput("T-1"));
    handler.handle(new OpenTableSessionInput("T-1"));

    verify(tableSessionRepository, org.mockito.Mockito.times(2)).save(sessionCaptor.capture());

    List<TableSession> saved = sessionCaptor.getAllValues();
    assertThat(saved).hasSize(2);

    String firstId = saved.getFirst().id().value();
    String secondId = saved.get(1).id().value();

    assertThat(firstId).isNotBlank();
    assertThat(secondId).isNotBlank();
    assertThat(firstId).isNotEqualTo(secondId);
  }

  @Test
  void handle_nullInput_throwsNpe() {
    OpenTableSessionHandler handler =
        new OpenTableSessionHandler(tableSessionRepository, new ApplicationCurrency("EUR"));
    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class, () -> handler.handle(null));

    assertThat(thrown.getMessage()).contains("input must not be null");
  }

  @Test
  void constructor_nullRepository_throwsNpe() {
    ApplicationCurrency applicationCurrency = new ApplicationCurrency("EUR");
    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class,
            () -> new OpenTableSessionHandler(null, applicationCurrency));

    assertThat(thrown.getMessage()).contains("tableSessionRepository must not be null");
  }

  @Test
  void constructor_nullApplicationCurrency_throwsNpe() {
    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class,
            () -> new OpenTableSessionHandler(tableSessionRepository, null));

    assertThat(thrown.getMessage()).contains("applicationCurrency must not be null");
  }
}
