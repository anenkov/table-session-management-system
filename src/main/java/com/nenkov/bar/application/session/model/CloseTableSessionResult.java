package com.nenkov.bar.application.session.model;

import com.nenkov.bar.domain.model.session.TableSessionId;
import com.nenkov.bar.domain.model.session.TableSessionStatus;
import java.time.Instant;
import java.util.Objects;

/** Result model for closing a table session. */
public record CloseTableSessionResult(
    TableSessionId sessionId, TableSessionStatus status, Instant closedAt) {

  public CloseTableSessionResult {
    Objects.requireNonNull(sessionId, "sessionId must not be null");
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(closedAt, "closedAt must not be null");
  }
}
