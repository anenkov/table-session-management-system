package com.nenkov.bar.application.ordering.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.nenkov.bar.application.ordering.model.AddOrderItemsInput;
import com.nenkov.bar.application.ordering.model.AddOrderItemsResult;
import java.util.List;
import org.junit.jupiter.api.Test;

final class AddOrderItemsHandlerTest {

  @Test
  void handle_nullInput_throwsNpe() {
    AddOrderItemsHandler handler = new AddOrderItemsHandler();

    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class, () -> handler.handle(null));

    assertThat(thrown.getMessage()).contains("input must not be null");
  }

  @Test
  void handle_nonNullInput_throwsUnsupportedOperationException() {
    AddOrderItemsHandler handler = new AddOrderItemsHandler();

    AddOrderItemsInput input =
        new AddOrderItemsInput("S-1", List.of(new AddOrderItemsInput.RequestedItem("P-1", 1)));

    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            UnsupportedOperationException.class, () -> handler.handle(input));

    assertThat(thrown.getMessage()).contains("AddOrderItemsHandler not implemented yet");
  }

  @Test
  void placeholderResult_echoesSessionId_andReturnsEmptyCreatedIds() {
    AddOrderItemsInput input =
        new AddOrderItemsInput("S-1", List.of(new AddOrderItemsInput.RequestedItem("P-1", 1)));

    AddOrderItemsResult result = AddOrderItemsHandler.placeholderResult(input);

    assertThat(result.sessionId()).isEqualTo("S-1");
    assertThat(result.createdItemIds()).isEmpty();
  }
}
