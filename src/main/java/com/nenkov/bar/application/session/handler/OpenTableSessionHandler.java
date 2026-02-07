package com.nenkov.bar.application.session.handler;

import com.nenkov.bar.application.session.model.OpenTableSessionInput;
import com.nenkov.bar.application.session.model.OpenTableSessionResult;
import com.nenkov.bar.application.session.repository.TableSessionRepository;
import com.nenkov.bar.domain.model.session.TableSession;
import com.nenkov.bar.domain.model.session.TableSessionContents;
import com.nenkov.bar.domain.model.session.TableSessionId;
import com.nenkov.bar.domain.model.session.TableSessionStatus;
import java.util.Objects;
import java.util.UUID;

/**
 * Workflow handler: open a new table session.
 *
 * <p>Orchestrates domain and repository only. Domain rules must remain in the domain.
 *
 * <p>Current limitation (intentional for skeleton stage):
 *
 * <ul>
 *   <li>Domain does not yet model TableId, so {@code input.tableId()} is application metadata only.
 *   <li>The locked {@link TableSessionRepository} supports lookup by {@link TableSessionId} only,
 *       so this handler cannot enforce "one open session per table" at this layer.
 * </ul>
 */
public final class OpenTableSessionHandler {

  // Single configured currency for the whole application.
  // Later this can come from application configuration (without changing domain).
  private static final String CURRENCY = "EUR";

  private final TableSessionRepository tableSessionRepository;

  public OpenTableSessionHandler(TableSessionRepository tableSessionRepository) {
    this.tableSessionRepository =
        Objects.requireNonNull(tableSessionRepository, "tableSessionRepository must not be null");
  }

  public OpenTableSessionResult handle(OpenTableSessionInput input) {
    Objects.requireNonNull(input, "input must not be null");

    TableSessionId sessionId = TableSessionId.of(UUID.randomUUID().toString());

    TableSession session =
        new TableSession(
            sessionId,
            CURRENCY,
            TableSessionContents.empty(), // session write-offs (none at session open)
            TableSessionStatus.OPEN,
            null // closedAt
            );

    tableSessionRepository.save(session);

    return new OpenTableSessionResult(sessionId.value(), input.tableId());
  }
}
