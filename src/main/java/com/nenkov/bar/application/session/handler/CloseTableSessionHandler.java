package com.nenkov.bar.application.session.handler;

import com.nenkov.bar.application.session.exception.TableSessionNotFoundException;
import com.nenkov.bar.application.session.model.CloseTableSessionInput;
import com.nenkov.bar.application.session.model.CloseTableSessionResult;
import com.nenkov.bar.application.session.repository.TableSessionRepository;
import com.nenkov.bar.domain.model.session.TableSession;
import java.time.Instant;
import java.util.Objects;

/**
 * Workflow handler: administratively close a table session.
 *
 * <p>Orchestrates repository + domain transition only. Business rules remain in domain.
 */
public final class CloseTableSessionHandler {

  private final TableSessionRepository tableSessionRepository;

  public CloseTableSessionHandler(TableSessionRepository tableSessionRepository) {
    this.tableSessionRepository =
        Objects.requireNonNull(tableSessionRepository, "tableSessionRepository must not be null");
  }

  public CloseTableSessionResult handle(CloseTableSessionInput input) {
    Objects.requireNonNull(input, "input must not be null");

    TableSession session =
        tableSessionRepository
            .findById(input.sessionId())
            .orElseThrow(() -> new TableSessionNotFoundException(input.sessionId()));

    Instant closedAt = Instant.now();
    TableSession closed = session.closeByManager(closedAt);

    tableSessionRepository.save(closed);

    return new CloseTableSessionResult(closed.id(), closed.status(), closed.closedAt());
  }
}
