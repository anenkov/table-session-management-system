package com.nenkov.bar.application.payment.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nenkov.bar.application.payment.exception.CheckCreationNotAllowedException;
import com.nenkov.bar.application.payment.exception.InvalidPaymentSelectionException;
import com.nenkov.bar.application.payment.model.CreateCheckInput;
import com.nenkov.bar.application.payment.model.CreateCheckResult;
import com.nenkov.bar.application.payment.repository.CheckRepository;
import com.nenkov.bar.application.session.exception.TableSessionNotFoundException;
import com.nenkov.bar.application.session.repository.TableSessionRepository;
import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.payment.Check;
import com.nenkov.bar.domain.model.payment.CheckQuote;
import com.nenkov.bar.domain.model.payment.PaidItem;
import com.nenkov.bar.domain.model.payment.PaymentSelection;
import com.nenkov.bar.domain.model.session.OrderItemId;
import com.nenkov.bar.domain.model.session.TableSession;
import com.nenkov.bar.domain.model.session.TableSessionContents;
import com.nenkov.bar.domain.model.session.TableSessionId;
import com.nenkov.bar.domain.model.session.TableSessionStatus;
import com.nenkov.bar.domain.service.payment.CheckAmountCalculator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
final class CreateCheckHandlerTest {

  @Mock private TableSessionRepository tableSessionRepository;
  @Mock private CheckRepository checkRepository;
  @Mock private CheckAmountCalculator checkAmountCalculator;

  @Captor private ArgumentCaptor<Check> checkCaptor;

  @Test
  void handle_happyPath_loadsSession_quotes_createsAndSavesCheck_andReturnsResult() {
    CreateCheckHandler handler =
        new CreateCheckHandler(tableSessionRepository, checkRepository, checkAmountCalculator);

    TableSessionId sessionId = TableSessionId.of("S-1");

    OrderItemId itemId = OrderItemId.random();
    Money unitPrice = Money.of("EUR", new BigDecimal("5.00"));

    TableSession session =
        new TableSession(
            sessionId, "EUR", TableSessionContents.empty(), TableSessionStatus.OPEN, null);

    List<PaymentSelection> selections = List.of(PaymentSelection.of(itemId, 2));
    CreateCheckInput input = new CreateCheckInput(sessionId, selections);

    Money checkAmount = Money.of("EUR", new BigDecimal("9.99"));
    PaidItem paidItem = PaidItem.of(itemId, 2, unitPrice, checkAmount);
    CheckQuote quote = CheckQuote.of(checkAmount, List.of(paidItem));

    when(tableSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
    when(checkAmountCalculator.quote(
            "EUR",
            session.payableItemsSnapshot(),
            selections,
            session.itemWriteOffs(),
            session.sessionWriteOffs()))
        .thenReturn(quote);

    Instant before = Instant.now();
    CreateCheckResult result = handler.handle(input);
    Instant after = Instant.now();

    // Verify quote call (arguments are important for correctness)
    verify(checkAmountCalculator)
        .quote(
            "EUR",
            session.payableItemsSnapshot(),
            selections,
            session.itemWriteOffs(),
            session.sessionWriteOffs());

    // Verify saved Check
    verify(checkRepository).save(checkCaptor.capture());
    Check saved = checkCaptor.getValue();

    assertThat(saved).isNotNull();
    assertThat(saved.sessionId()).isEqualTo(sessionId);
    assertThat(saved.amount()).isEqualTo(checkAmount);
    assertThat(saved.paidItems()).isEqualTo(quote.paidItems());

    assertThat(saved.id()).isNotNull();
    assertThat(saved.createdAt()).isNotNull();
    assertThat(saved.createdAt()).isBetween(before.minusSeconds(1), after.plusSeconds(1));

    // Verify returned result
    assertThat(result.sessionId()).isEqualTo(sessionId);
    assertThat(result.checkId()).isEqualTo(saved.id());
    assertThat(result.amount()).isEqualTo(checkAmount);
  }

  @Test
  void handle_whenSessionMissing_throwsNotFound_andDoesNotQuoteOrSave() {
    CreateCheckHandler handler =
        new CreateCheckHandler(tableSessionRepository, checkRepository, checkAmountCalculator);

    TableSessionId sessionId = TableSessionId.of("missing");
    CreateCheckInput input =
        new CreateCheckInput(sessionId, List.of(PaymentSelection.of(OrderItemId.random(), 1)));

    when(tableSessionRepository.findById(sessionId)).thenReturn(Optional.empty());

    Throwable thrown =
        assertThrows(TableSessionNotFoundException.class, () -> handler.handle(input));

    assertThat(thrown.getMessage()).contains("TableSession not found: " + sessionId.value());

    verify(checkAmountCalculator, never()).quote(any(), any(), any(), any(), any());
    verify(checkRepository, never()).save(any());
  }

  @Test
  void handle_whenSessionClosed_throwsCheckCreationNotAllowed_andDoesNotQuoteOrSave() {
    CreateCheckHandler handler =
        new CreateCheckHandler(tableSessionRepository, checkRepository, checkAmountCalculator);

    TableSessionId sessionId = TableSessionId.of("S-closed");
    OrderItemId itemId = OrderItemId.random();
    List<PaymentSelection> selections = List.of(PaymentSelection.of(itemId, 1));
    CreateCheckInput input = new CreateCheckInput(sessionId, selections);

    TableSession session =
        new TableSession(
            sessionId,
            "EUR",
            TableSessionContents.empty(),
            TableSessionStatus.CLOSED,
            Instant.parse("2026-02-09T00:00:00Z"));

    when(tableSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

    assertThrows(CheckCreationNotAllowedException.class, () -> handler.handle(input));

    verify(checkAmountCalculator, never()).quote(any(), any(), any(), any(), any());
    verify(checkRepository, never()).save(any());
  }

  @Test
  void
      handle_whenCalculatorThrowsIllegalArgumentException_mapsToInvalidPaymentSelection_andDoesNotSaveCheck() {
    CreateCheckHandler handler =
        new CreateCheckHandler(tableSessionRepository, checkRepository, checkAmountCalculator);

    TableSessionId sessionId = TableSessionId.of("S-1");
    OrderItemId itemId = OrderItemId.random();

    TableSession session =
        new TableSession(
            sessionId, "EUR", TableSessionContents.empty(), TableSessionStatus.OPEN, null);

    List<PaymentSelection> selections = List.of(PaymentSelection.of(itemId, 1));
    CreateCheckInput input = new CreateCheckInput(sessionId, selections);

    when(tableSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

    when(checkAmountCalculator.quote(
            "EUR",
            session.payableItemsSnapshot(),
            selections,
            session.itemWriteOffs(),
            session.sessionWriteOffs()))
        .thenThrow(new IllegalArgumentException("invalid selection"));

    assertThrows(InvalidPaymentSelectionException.class, () -> handler.handle(input));

    verify(checkRepository, never()).save(any());
  }

  @Test
  void handle_whenCalculatorThrows_propagates_andDoesNotSaveCheck() {
    CreateCheckHandler handler =
        new CreateCheckHandler(tableSessionRepository, checkRepository, checkAmountCalculator);

    TableSessionId sessionId = TableSessionId.of("S-1");

    OrderItemId itemId = OrderItemId.random();

    TableSession session =
        new TableSession(
            sessionId, "EUR", TableSessionContents.empty(), TableSessionStatus.OPEN, null);

    List<PaymentSelection> selections = List.of(PaymentSelection.of(itemId, 1));
    CreateCheckInput input = new CreateCheckInput(sessionId, selections);

    when(tableSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

    RuntimeException boom = new RuntimeException("calculator failed");
    when(checkAmountCalculator.quote(
            "EUR",
            session.payableItemsSnapshot(),
            selections,
            session.itemWriteOffs(),
            session.sessionWriteOffs()))
        .thenThrow(boom);

    Throwable thrown = assertThrows(RuntimeException.class, () -> handler.handle(input));

    assertThat(thrown).isSameAs(boom);
    verify(checkRepository, never()).save(any());
  }

  @Test
  void handle_nullInput_throwsNpe() {
    CreateCheckHandler handler =
        new CreateCheckHandler(tableSessionRepository, checkRepository, checkAmountCalculator);

    Throwable thrown = assertThrows(NullPointerException.class, () -> handler.handle(null));

    assertThat(thrown.getMessage()).contains("input must not be null");
  }

  @Test
  void constructor_nullSessionRepo_throwsNpe() {
    Throwable thrown =
        assertThrows(
            NullPointerException.class,
            () -> new CreateCheckHandler(null, checkRepository, checkAmountCalculator));

    assertThat(thrown.getMessage()).contains("tableSessionRepository must not be null");
  }

  @Test
  void constructor_nullCheckRepo_throwsNpe() {
    Throwable thrown =
        assertThrows(
            NullPointerException.class,
            () -> new CreateCheckHandler(tableSessionRepository, null, checkAmountCalculator));

    assertThat(thrown.getMessage()).contains("checkRepository must not be null");
  }

  @Test
  void constructor_nullCalculator_throwsNpe() {
    Throwable thrown =
        assertThrows(
            NullPointerException.class,
            () -> new CreateCheckHandler(tableSessionRepository, checkRepository, null));

    assertThat(thrown.getMessage()).contains("checkAmountCalculator must not be null");
  }
}
