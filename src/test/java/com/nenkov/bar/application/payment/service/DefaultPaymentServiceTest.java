package com.nenkov.bar.application.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nenkov.bar.application.payment.handler.CreateCheckHandler;
import com.nenkov.bar.application.payment.handler.RecordPaymentAttemptHandler;
import com.nenkov.bar.application.payment.model.CreateCheckInput;
import com.nenkov.bar.application.payment.model.CreateCheckResult;
import com.nenkov.bar.application.payment.model.PaymentRequestId;
import com.nenkov.bar.application.payment.model.RecordPaymentAttemptInput;
import com.nenkov.bar.application.payment.model.RecordPaymentAttemptResult;
import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.payment.CheckId;
import com.nenkov.bar.domain.model.session.OrderItemId;
import com.nenkov.bar.domain.model.session.TableSessionId;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
final class DefaultPaymentServiceTest {

  @Mock private CreateCheckHandler createCheckHandler;
  @Mock private RecordPaymentAttemptHandler recordPaymentAttemptHandler;

  @Test
  void createCheck_delegatesToHandler() {
    DefaultPaymentService service =
        new DefaultPaymentService(createCheckHandler, recordPaymentAttemptHandler);

    TableSessionId sessionId = TableSessionId.of("S-1");
    CreateCheckInput input =
        new CreateCheckInput(
            sessionId,
            List.of(
                com.nenkov.bar.domain.model.payment.PaymentSelection.of(OrderItemId.random(), 1)));

    CreateCheckResult expected =
        new CreateCheckResult(
            sessionId, CheckId.of(UUID.randomUUID()), Money.of("EUR", new BigDecimal("1.00")));

    when(createCheckHandler.handle(input)).thenReturn(expected);

    CreateCheckResult actual = service.createCheck(input);

    assertThat(actual).isSameAs(expected);
    verify(createCheckHandler).handle(input);
  }

  @Test
  void recordPaymentAttempt_delegatesToHandler() {
    DefaultPaymentService service =
        new DefaultPaymentService(createCheckHandler, recordPaymentAttemptHandler);

    PaymentRequestId requestId = PaymentRequestId.of("req-1");
    TableSessionId sessionId = TableSessionId.of("S-1");
    CheckId checkId = CheckId.of(UUID.randomUUID());

    RecordPaymentAttemptInput input = new RecordPaymentAttemptInput(requestId, sessionId, checkId);

    RecordPaymentAttemptResult expected =
        new RecordPaymentAttemptResult(
            requestId,
            sessionId,
            checkId,
            com.nenkov.bar.application.payment.model.PaymentAttemptResult.pending("prov-x"));

    when(recordPaymentAttemptHandler.handle(input)).thenReturn(expected);

    RecordPaymentAttemptResult actual = service.recordPaymentAttempt(input);

    assertThat(actual).isSameAs(expected);
    verify(recordPaymentAttemptHandler).handle(input);
  }

  @Test
  void constructor_nullCreateHandler_throwsNpe() {
    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class,
            () -> new DefaultPaymentService(null, recordPaymentAttemptHandler));

    assertThat(thrown.getMessage()).contains("createCheckHandler must not be null");
  }

  @Test
  void constructor_nullRecordHandler_throwsNpe() {
    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class, () -> new DefaultPaymentService(createCheckHandler, null));

    assertThat(thrown.getMessage()).contains("recordPaymentAttemptHandler must not be null");
  }
}
