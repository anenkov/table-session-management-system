package com.nenkov.bar.application.ordering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nenkov.bar.application.ordering.handler.AddOrderItemsHandler;
import com.nenkov.bar.application.ordering.model.AddOrderItemsInput;
import com.nenkov.bar.application.ordering.model.AddOrderItemsResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
final class DefaultOrderingServiceTest {

  @Mock private AddOrderItemsHandler addOrderItemsHandler;

  @Test
  void addItems_delegatesToHandler() {
    DefaultOrderingService service = new DefaultOrderingService(addOrderItemsHandler);

    AddOrderItemsInput input =
        new AddOrderItemsInput("S-1", List.of(new AddOrderItemsInput.RequestedItem("P-1", 2)));

    AddOrderItemsResult expected = new AddOrderItemsResult("S-1", List.of("OI-1"));
    when(addOrderItemsHandler.handle(input)).thenReturn(expected);

    AddOrderItemsResult actual = service.addItems(input);

    assertThat(actual).isSameAs(expected);
    verify(addOrderItemsHandler).handle(input);
  }

  @Test
  void constructor_nullHandler_throwsNpe() {
    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class, () -> new DefaultOrderingService(null));

    assertThat(thrown.getMessage()).contains("addOrderItemsHandler must not be null");
  }
}
