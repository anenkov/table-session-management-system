package com.nenkov.bar.application.session.handler;

import com.nenkov.bar.application.common.config.ApplicationCurrency;
import com.nenkov.bar.application.session.exception.TableAlreadyHasOpenSessionException;
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
 * <p>Current limitation:
 *
 * <ul>
 *   <li>Domain does not yet model TableId, so {@code input.tableId()} is application metadata only.
 * </ul>
 */
public final class OpenTableSessionHandler {

  private final TableSessionRepository tableSessionRepository;
  private final ApplicationCurrency applicationCurrency;

  public OpenTableSessionHandler(
      TableSessionRepository tableSessionRepository, ApplicationCurrency applicationCurrency) {

    this.tableSessionRepository =
        Objects.requireNonNull(tableSessionRepository, "tableSessionRepository must not be null");
    this.applicationCurrency =
        Objects.requireNonNull(applicationCurrency, "applicationCurrency must not be null");
  }

  public OpenTableSessionResult handle(OpenTableSessionInput input) {
    Objects.requireNonNull(input, "input must not be null");

    boolean hasOpenSession = tableSessionRepository.existsOpenByTableId(input.tableId());
    if (hasOpenSession) {
      throw new TableAlreadyHasOpenSessionException(input.tableId());
    }

    TableSessionId sessionId = TableSessionId.of(UUID.randomUUID().toString());

    TableSession session =
        new TableSession(
            sessionId,
            applicationCurrency.code(),
            TableSessionContents.empty(), // session write-offs (none at session open)
            TableSessionStatus.OPEN,
            null // closedAt
            );

    tableSessionRepository.save(session);

    return new OpenTableSessionResult(sessionId.value(), input.tableId());
  }
}
