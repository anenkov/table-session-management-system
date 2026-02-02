package com.nenkov.bar.application.session.model;

import com.nenkov.bar.domain.model.session.TableSessionId;
import java.util.Objects;

/** Input model for loading a table session by id. */
public record GetTableSessionInput(TableSessionId sessionId) {

  public GetTableSessionInput {
    Objects.requireNonNull(sessionId, "sessionId must not be null");
  }
}
