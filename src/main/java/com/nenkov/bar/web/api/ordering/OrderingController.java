package com.nenkov.bar.web.api.ordering;

import com.nenkov.bar.application.ordering.model.AddOrderItemsInput;
import com.nenkov.bar.application.ordering.model.AddOrderItemsResult;
import com.nenkov.bar.application.ordering.service.OrderingService;
import com.nenkov.bar.domain.model.session.TableSessionId;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Ordering HTTP API. */
@RestController
@RequestMapping(path = "/sessions", produces = MediaType.APPLICATION_JSON_VALUE)
public final class OrderingController {

  private final OrderingService orderingService;

  public OrderingController(OrderingService orderingService) {
    this.orderingService = orderingService;
  }

  @PostMapping(path = "/{sessionId}/orders/items", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<AddOrderItemsResponse> addOrderItems(
      @PathVariable TableSessionId sessionId, @Valid @RequestBody AddOrderItemsRequest request) {

    return Mono.fromSupplier(
        () -> {
          List<AddOrderItemsInput.RequestedItem> items =
              request.items().stream()
                  .map(i -> new AddOrderItemsInput.RequestedItem(i.productId(), i.quantity()))
                  .toList();

          AddOrderItemsResult result =
              orderingService.addItems(new AddOrderItemsInput(sessionId.value(), items));

          return new AddOrderItemsResponse(result.sessionId(), result.createdItemIds());
        });
  }
}
