package com.nenkov.bar.web.api.conversion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nenkov.bar.domain.model.session.TableSessionId;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class StringToTableSessionIdConverterTest {

  private final StringToTableSessionIdConverter converter = new StringToTableSessionIdConverter();

  @Test
  void convert_validValue_returnsTypedId() {
    TableSessionId id = converter.convert("S-1");

    assertThat(id).isEqualTo(TableSessionId.of("S-1"));
  }

  @Test
  void convert_blankValue_throwsBadRequest() {
    assertThatThrownBy(() -> converter.convert(" "))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            ex -> {
              ResponseStatusException rse = (ResponseStatusException) ex;
              assertThat(rse.getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value());
              assertThat(rse.getReason()).isEqualTo("Invalid sessionId.");
            });
  }
}
