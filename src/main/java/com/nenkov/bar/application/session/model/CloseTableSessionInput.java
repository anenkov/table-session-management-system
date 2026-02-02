package com.nenkov.bar.application.session.model;

import com.nenkov.bar.domain.model.session.TableSessionId;
import java.util.Objects;

/**
 * Input model for administratively closing a table session.
 *
 * <p>Manager-only enforcement will happen at the API/auth layer later. For now this models intent.
 */
public record CloseTableSessionInput(TableSessionId sessionId) {

  public CloseTableSessionInput {
    Objects.requireNonNull(sessionId, "sessionId must not be null");
  }
}
