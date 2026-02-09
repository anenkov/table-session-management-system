package com.nenkov.bar.web.api.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.nenkov.bar.application.payment.exception.CheckCreationNotAllowedException;
import com.nenkov.bar.application.payment.exception.InvalidPaymentSelectionException;
import com.nenkov.bar.application.session.exception.TableAlreadyHasOpenSessionException;
import com.nenkov.bar.application.session.exception.TableSessionNotFoundException;
import com.nenkov.bar.auth.InvalidCredentialsException;
import com.nenkov.bar.domain.exceptions.OrderingNotAllowedException;
import com.nenkov.bar.domain.model.session.TableSessionId;
import java.util.List;
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
                new InvalidPaymentSelectionExceptionMapper()));

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
  }
}
