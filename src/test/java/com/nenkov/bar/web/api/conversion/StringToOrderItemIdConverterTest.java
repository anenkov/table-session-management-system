package com.nenkov.bar.web.api.conversion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nenkov.bar.domain.model.session.OrderItemId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class StringToOrderItemIdConverterTest {

  private final StringToOrderItemIdConverter converter = new StringToOrderItemIdConverter();

  @Test
  void convert_validUuid_returnsTypedId() {
    UUID raw = UUID.randomUUID();

    OrderItemId id = converter.convert(raw.toString());

    assertThat(id).isEqualTo(OrderItemId.of(raw));
  }

  @Test
  void convert_invalidUuid_throwsBadRequest() {
    assertThatThrownBy(() -> converter.convert("not-a-uuid"))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            ex -> {
              ResponseStatusException rse = (ResponseStatusException) ex;
              assertThat(rse.getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value());
              assertThat(rse.getReason()).isEqualTo("Invalid itemId.");
            });
  }
}

