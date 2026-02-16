package com.nenkov.bar.web.api.conversion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nenkov.bar.application.payment.model.PaymentRequestId;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class StringToPaymentRequestIdConverterTest {

  private final StringToPaymentRequestIdConverter converter =
      new StringToPaymentRequestIdConverter();

  @Test
  void convert_validValue_returnsTypedId() {
    PaymentRequestId id = converter.convert("req-1");

    assertThat(id).isEqualTo(PaymentRequestId.of("req-1"));
  }

  @Test
  void convert_blankValue_throwsBadRequest() {
    assertThatThrownBy(() -> converter.convert(" "))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            ex -> {
              ResponseStatusException rse = (ResponseStatusException) ex;
              assertThat(rse.getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value());
              assertThat(rse.getReason()).isEqualTo("Invalid requestId.");
            });
  }
}

