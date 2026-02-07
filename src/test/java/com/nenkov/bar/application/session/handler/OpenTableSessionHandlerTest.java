package com.nenkov.bar.application.session.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.nenkov.bar.application.common.config.ApplicationCurrency;
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
  void handle_persistsOpenSessionWithEurEmptyState_andReturnsIds() {
    OpenTableSessionHandler handler =
        new OpenTableSessionHandler(tableSessionRepository, new ApplicationCurrency("EUR"));

    OpenTableSessionInput input = new OpenTableSessionInput("T-12");

    OpenTableSessionResult result = handler.handle(input);

    // persists exactly one new session
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

    // returns identifiers needed by caller
    assertThat(result.sessionId()).isEqualTo(saved.id().value());
    assertThat(result.tableId()).isEqualTo("T-12");
  }

  @Test
  void handle_generatesNewSessionIdEveryTime() {
    OpenTableSessionHandler handler =
        new OpenTableSessionHandler(tableSessionRepository, new ApplicationCurrency("EUR"));
    handler.handle(new OpenTableSessionInput("T-1"));
    handler.handle(new OpenTableSessionInput("T-1"));

    verify(tableSessionRepository, org.mockito.Mockito.times(2)).save(sessionCaptor.capture());

    java.util.List<TableSession> saved = sessionCaptor.getAllValues();
    assertThat(saved).hasSize(2);

    String firstId = saved.get(0).id().value();
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
