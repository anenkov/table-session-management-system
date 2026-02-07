package com.nenkov.bar.application.ordering.handler;

import com.nenkov.bar.application.ordering.model.AddOrderItemsInput;
import com.nenkov.bar.application.ordering.model.AddOrderItemsResult;
import com.nenkov.bar.application.session.exception.TableSessionNotFoundException;
import com.nenkov.bar.application.session.repository.TableSessionRepository;
import com.nenkov.bar.domain.model.session.OrderItemDraft;
import com.nenkov.bar.domain.model.session.OrderItemsAdded;
import com.nenkov.bar.domain.model.session.TableSession;
import com.nenkov.bar.domain.model.session.TableSessionId;
import java.util.List;
import java.util.Objects;

/**
 * Workflow handler: add items to an existing table session.
 *
 * <p>Orchestrates repository + domain mutation. No framework annotations.
 */
public final class AddOrderItemsHandler {

  private final TableSessionRepository tableSessionRepository;

  public AddOrderItemsHandler(TableSessionRepository tableSessionRepository) {
    this.tableSessionRepository =
        Objects.requireNonNull(tableSessionRepository, "tableSessionRepository must not be null");
  }

  public AddOrderItemsResult handle(AddOrderItemsInput input) {
    Objects.requireNonNull(input, "input must not be null");

    TableSessionId sessionId = TableSessionId.of(input.sessionId());

    TableSession session =
        tableSessionRepository
            .findById(sessionId)
            .orElseThrow(() -> new TableSessionNotFoundException(sessionId));

    List<OrderItemDraft> drafts =
        input.items().stream().map(i -> new OrderItemDraft(i.productId(), i.quantity())).toList();

    OrderItemsAdded added = session.addOrderItems(drafts);

    tableSessionRepository.save(added.session());

    List<String> createdItemIds =
        added.createdOrderItemIds().stream().map(id -> id.value().toString()).toList();

    return new AddOrderItemsResult(input.sessionId(), createdItemIds);
  }
}
