package com.nenkov.bar.web.api.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.nenkov.bar.application.payment.exception.CheckCreationNotAllowedException;
import com.nenkov.bar.application.payment.exception.CheckNotFoundException;
import com.nenkov.bar.application.payment.exception.InvalidPaymentSelectionException;
import com.nenkov.bar.application.payment.exception.PaymentRequestIdConflictException;
import com.nenkov.bar.application.payment.model.PaymentRequestId;
import com.nenkov.bar.application.session.exception.TableAlreadyHasOpenSessionException;
import com.nenkov.bar.application.session.exception.TableSessionNotFoundException;
import com.nenkov.bar.auth.InvalidCredentialsException;
import com.nenkov.bar.domain.exceptions.OrderingNotAllowedException;
import com.nenkov.bar.domain.model.payment.CheckId;
import com.nenkov.bar.domain.model.session.TableSessionId;
import com.nenkov.bar.web.api.error.mapping.ApiExceptionMapperRegistry;
import com.nenkov.bar.web.api.error.mapping.CheckCreationNotAllowedExceptionMapper;
import com.nenkov.bar.web.api.error.mapping.CheckNotFoundExceptionMapper;
import com.nenkov.bar.web.api.error.mapping.InvalidCredentialsExceptionMapper;
import com.nenkov.bar.web.api.error.mapping.InvalidPaymentSelectionExceptionMapper;
import com.nenkov.bar.web.api.error.mapping.OrderingNotAllowedExceptionMapper;
import com.nenkov.bar.web.api.error.mapping.PaymentRequestIdConflictExceptionMapper;
import com.nenkov.bar.web.api.error.mapping.TableAlreadyHasOpenSessionExceptionMapper;
import com.nenkov.bar.web.api.error.mapping.TableSessionNotFoundExceptionMapper;
import com.nenkov.bar.web.api.error.model.ApiProblemCode;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApiExceptionMapperRegistryTest {

  @Test
  void shouldResolveExactMapperForKnownApiExceptions() {
    ApiExceptionMapperRegistry registry =
        new ApiExceptionMapperRegistry(
            List.of(
                new InvalidCredentialsExceptionMapper(),
                new TableSessionNotFoundExceptionMapper(),
                new TableAlreadyHasOpenSessionExceptionMapper(),
                new OrderingNotAllowedExceptionMapper(),
                new CheckCreationNotAllowedExceptionMapper(),
                new InvalidPaymentSelectionExceptionMapper(),
                new CheckNotFoundExceptionMapper(),
                new PaymentRequestIdConflictExceptionMapper()));

    assertThat(registry.findExact(new InvalidCredentialsException()).orElseThrow().code())
        .isEqualTo(ApiProblemCode.AUTH_INVALID_CREDENTIALS);

    assertThat(
            registry
                .findExact(new TableSessionNotFoundException(TableSessionId.of("x")))
                .orElseThrow()
                .code())
        .isEqualTo(ApiProblemCode.SESSION_NOT_FOUND);

    assertThat(
            registry.findExact(new TableAlreadyHasOpenSessionException("t")).orElseThrow().code())
        .isEqualTo(ApiProblemCode.SESSION_ALREADY_OPEN_FOR_TABLE);

    assertThat(registry.findExact(new OrderingNotAllowedException("x")).orElseThrow().code())
        .isEqualTo(ApiProblemCode.ORDERING_CONFLICT);

    assertThat(
            registry
                .findExact(new CheckCreationNotAllowedException(TableSessionId.of("x")))
                .orElseThrow()
                .code())
        .isEqualTo(ApiProblemCode.PAYMENT_CONFLICT);

    assertThat(
            registry
                .findExact(new InvalidPaymentSelectionException(TableSessionId.of("x")))
                .orElseThrow()
                .code())
        .isEqualTo(ApiProblemCode.PAYMENT_SELECTION_INVALID);

    assertThat(
            registry
                .findExact(
                    new CheckNotFoundException(
                        CheckId.of(UUID.fromString("11111111-1111-1111-1111-111111111111"))))
                .orElseThrow()
                .code())
        .isEqualTo(ApiProblemCode.CHECK_NOT_FOUND);

    assertThat(
            registry
                .findExact(
                    new PaymentRequestIdConflictException(
                        PaymentRequestId.of("req-1"),
                        TableSessionId.of("x"),
                        CheckId.of(UUID.fromString("11111111-1111-1111-1111-111111111111")),
                        TableSessionId.of("y"),
                        CheckId.of(UUID.fromString("22222222-2222-2222-2222-222222222222"))))
                .orElseThrow()
                .code())
        .isEqualTo(ApiProblemCode.PAYMENT_REQUEST_CONFLICT);
  }
}
