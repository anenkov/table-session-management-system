package com.nenkov.bar.application.session.handler;

import com.nenkov.bar.application.session.exception.TableSessionNotFoundException;
import com.nenkov.bar.application.session.model.GetTableSessionInput;
import com.nenkov.bar.application.session.model.GetTableSessionResult;
import com.nenkov.bar.application.session.repository.TableSessionRepository;
import com.nenkov.bar.domain.model.session.TableSession;
import java.util.Objects;

/**
 * Workflow handler: load a table session and expose a read-only projection.
 *
 * <p>Orchestrates repository only; no domain logic.
 */
public final class GetTableSessionHandler {

  private final TableSessionRepository tableSessionRepository;

  public GetTableSessionHandler(TableSessionRepository tableSessionRepository) {
    this.tableSessionRepository =
        Objects.requireNonNull(tableSessionRepository, "tableSessionRepository must not be null");
  }

  public GetTableSessionResult handle(GetTableSessionInput input) {
    Objects.requireNonNull(input, "input must not be null");

    TableSession session =
        tableSessionRepository
            .findById(input.sessionId())
            .orElseThrow(() -> new TableSessionNotFoundException(input.sessionId()));

    return new GetTableSessionResult(
        session.id(),
        session.currency(),
        session.payableItemsSnapshot(),
        session.itemWriteOffs(),
        session.sessionWriteOffs());
  }
}
