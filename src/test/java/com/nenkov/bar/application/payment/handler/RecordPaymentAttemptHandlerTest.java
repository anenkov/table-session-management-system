package com.nenkov.bar.application.payment.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nenkov.bar.application.payment.exception.CheckNotFoundException;
import com.nenkov.bar.application.payment.gateway.PaymentGateway;
import com.nenkov.bar.application.payment.model.PaymentAttemptResult;
import com.nenkov.bar.application.payment.model.PaymentRequestId;
import com.nenkov.bar.application.payment.model.RecordPaymentAttemptInput;
import com.nenkov.bar.application.payment.model.RecordPaymentAttemptResult;
import com.nenkov.bar.application.payment.repository.CheckRepository;
import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.payment.Check;
import com.nenkov.bar.domain.model.payment.CheckId;
import com.nenkov.bar.domain.model.payment.CheckStatus;
import com.nenkov.bar.domain.model.payment.PaidItem;
import com.nenkov.bar.domain.model.session.OrderItemId;
import com.nenkov.bar.domain.model.session.TableSessionId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
final class RecordPaymentAttemptHandlerTest {

  @Mock private PaymentGateway paymentGateway;
  @Mock private CheckRepository checkRepository;

  @Captor private ArgumentCaptor<Check> savedCheckCaptor;

  @Test
  void handle_whenApproved_marksPaid_andSaves() {
    RecordPaymentAttemptHandler handler =
        new RecordPaymentAttemptHandler(paymentGateway, checkRepository);

    PaymentRequestId requestId = PaymentRequestId.of("req-1");
    TableSessionId sessionId = TableSessionId.of("S-1");
    CheckId checkId = CheckId.of(UUID.randomUUID());

    Money amount = Money.of("EUR", new BigDecimal("12.34"));
    OrderItemId itemId = OrderItemId.random();
    PaidItem paidItem = PaidItem.of(itemId, 1, amount, amount);

    Check check =
        Check.create(
            sessionId, checkId, amount, List.of(paidItem), Instant.parse("2026-01-01T00:00:00Z"));

    when(checkRepository.findById(checkId)).thenReturn(Optional.of(check));

    PaymentAttemptResult approved = PaymentAttemptResult.approved("prov-123");
    when(paymentGateway.initiatePayment(requestId, sessionId, checkId, amount))
        .thenReturn(approved);

    Instant before = Instant.now();
    RecordPaymentAttemptResult result =
        handler.handle(new RecordPaymentAttemptInput(requestId, sessionId, checkId));
    Instant after = Instant.now();

    // verify gateway call is correct
    verify(paymentGateway).initiatePayment(requestId, sessionId, checkId, amount);

    // approved -> save
    verify(checkRepository).save(savedCheckCaptor.capture());
    Check saved = savedCheckCaptor.getValue();

    assertThat(saved).isSameAs(check);
    assertThat(saved.status()).isEqualTo(CheckStatus.PAID);
    assertThat(saved.paymentReference()).isNotNull();
    assertThat(saved.paymentReference().value()).isEqualTo("prov-123");
    assertThat(saved.completedAt()).isNotNull();
    assertThat(saved.completedAt()).isBetween(before.minusSeconds(1), after.plusSeconds(1));

    // result mirrors attempt
    assertThat(result.requestId()).isEqualTo(requestId);
    assertThat(result.sessionId()).isEqualTo(sessionId);
    assertThat(result.checkId()).isEqualTo(checkId);
    assertThat(result.attemptResult()).isSameAs(approved);
  }

  @Test
  void handle_whenDeclined_marksFailed_andSaves() {
    RecordPaymentAttemptHandler handler =
        new RecordPaymentAttemptHandler(paymentGateway, checkRepository);

    PaymentRequestId requestId = PaymentRequestId.of("req-2");
    TableSessionId sessionId = TableSessionId.of("S-1");
    CheckId checkId = CheckId.of(UUID.randomUUID());

    Money amount = Money.of("EUR", new BigDecimal("10.00"));
    OrderItemId itemId = OrderItemId.random();
    PaidItem paidItem = PaidItem.of(itemId, 1, amount, amount);

    Check check =
        Check.create(
            sessionId, checkId, amount, List.of(paidItem), Instant.parse("2026-01-01T00:00:00Z"));

    when(checkRepository.findById(checkId)).thenReturn(Optional.of(check));

    PaymentAttemptResult declined = PaymentAttemptResult.declined("prov-456", "no funds");
    when(paymentGateway.initiatePayment(requestId, sessionId, checkId, amount))
        .thenReturn(declined);

    Instant before = Instant.now();
    RecordPaymentAttemptResult result =
        handler.handle(new RecordPaymentAttemptInput(requestId, sessionId, checkId));
    Instant after = Instant.now();

    verify(paymentGateway).initiatePayment(requestId, sessionId, checkId, amount);

    verify(checkRepository).save(savedCheckCaptor.capture());
    Check saved = savedCheckCaptor.getValue();

    assertThat(saved).isSameAs(check);
    assertThat(saved.status()).isEqualTo(CheckStatus.FAILED);
    assertThat(saved.paymentReference()).isNull(); // declined path does not set reference
    assertThat(saved.completedAt()).isNotNull();
    assertThat(saved.completedAt()).isBetween(before.minusSeconds(1), after.plusSeconds(1));

    assertThat(result.attemptResult()).isSameAs(declined);
    assertThat(result.attemptResult().failureReason()).contains("no funds");
  }

  @Test
  void handle_whenPending_doesNotChangeState_andDoesNotSave() {
    RecordPaymentAttemptHandler handler =
        new RecordPaymentAttemptHandler(paymentGateway, checkRepository);

    PaymentRequestId requestId = PaymentRequestId.of("req-3");
    TableSessionId sessionId = TableSessionId.of("S-1");
    CheckId checkId = CheckId.of(UUID.randomUUID());

    Money amount = Money.of("EUR", new BigDecimal("7.00"));
    OrderItemId itemId = OrderItemId.random();
    PaidItem paidItem = PaidItem.of(itemId, 1, amount, amount);

    Check check =
        Check.create(
            sessionId, checkId, amount, List.of(paidItem), Instant.parse("2026-01-01T00:00:00Z"));
    assertThat(check.status()).isEqualTo(CheckStatus.CREATED);
    assertThat(check.completedAt()).isNull();

    when(checkRepository.findById(checkId)).thenReturn(Optional.of(check));

    PaymentAttemptResult pending = PaymentAttemptResult.pending("prov-789");
    when(paymentGateway.initiatePayment(requestId, sessionId, checkId, amount)).thenReturn(pending);

    RecordPaymentAttemptResult result =
        handler.handle(new RecordPaymentAttemptInput(requestId, sessionId, checkId));

    verify(paymentGateway).initiatePayment(requestId, sessionId, checkId, amount);

    // pending -> no save, no state transition
    verify(checkRepository, never()).save(org.mockito.ArgumentMatchers.any());

    assertThat(check.status()).isEqualTo(CheckStatus.CREATED);
    assertThat(check.completedAt()).isNull();

    assertThat(result.attemptResult()).isSameAs(pending);
  }

  @Test
  void handle_whenCheckMissing_throwsNotFound_andDoesNotCallGatewayOrSave() {
    RecordPaymentAttemptHandler handler =
        new RecordPaymentAttemptHandler(paymentGateway, checkRepository);

    PaymentRequestId requestId = PaymentRequestId.of("req-4");
    TableSessionId sessionId = TableSessionId.of("S-1");
    CheckId checkId = CheckId.of(UUID.randomUUID());
    RecordPaymentAttemptInput input = new RecordPaymentAttemptInput(requestId, sessionId, checkId);

    when(checkRepository.findById(checkId)).thenReturn(Optional.empty());

    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            CheckNotFoundException.class, () -> handler.handle(input));

    assertThat(thrown.getMessage()).contains("Check not found: " + checkId.value());

    verify(paymentGateway, never())
        .initiatePayment(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());

    verify(checkRepository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void handle_nullInput_throwsNpe() {
    RecordPaymentAttemptHandler handler =
        new RecordPaymentAttemptHandler(paymentGateway, checkRepository);

    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class, () -> handler.handle(null));

    assertThat(thrown.getMessage()).contains("input must not be null");
  }

  @Test
  void constructor_nullGateway_throwsNpe() {
    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class,
            () -> new RecordPaymentAttemptHandler(null, checkRepository));

    assertThat(thrown.getMessage()).contains("paymentGateway must not be null");
  }

  @Test
  void constructor_nullRepository_throwsNpe() {
    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class,
            () -> new RecordPaymentAttemptHandler(paymentGateway, null));

    assertThat(thrown.getMessage()).contains("checkRepository must not be null");
  }
}
