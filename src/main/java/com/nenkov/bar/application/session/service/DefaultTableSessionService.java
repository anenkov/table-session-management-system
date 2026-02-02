package com.nenkov.bar.application.session.service;

import com.nenkov.bar.application.session.handler.CloseTableSessionHandler;
import com.nenkov.bar.application.session.handler.GetTableSessionHandler;
import com.nenkov.bar.application.session.handler.OpenTableSessionHandler;
import com.nenkov.bar.application.session.model.CloseTableSessionInput;
import com.nenkov.bar.application.session.model.CloseTableSessionResult;
import com.nenkov.bar.application.session.model.GetTableSessionInput;
import com.nenkov.bar.application.session.model.GetTableSessionResult;
import com.nenkov.bar.application.session.model.OpenTableSessionInput;
import com.nenkov.bar.application.session.model.OpenTableSessionResult;
import java.util.Objects;

/**
 * Default implementation of {@link TableSessionService}.
 *
 * <p>Thin feature façade: delegates workflows to handlers.
 */
public final class DefaultTableSessionService implements TableSessionService {

  private final OpenTableSessionHandler openTableSessionHandler;
  private final GetTableSessionHandler getTableSessionHandler;
  private final CloseTableSessionHandler closeTableSessionHandler;

  public DefaultTableSessionService(
      OpenTableSessionHandler openTableSessionHandler,
      GetTableSessionHandler getTableSessionHandler,
      CloseTableSessionHandler closeTableSessionHandler) {

    this.openTableSessionHandler =
        Objects.requireNonNull(openTableSessionHandler, "openTableSessionHandler must not be null");
    this.getTableSessionHandler =
        Objects.requireNonNull(getTableSessionHandler, "getTableSessionHandler must not be null");
    this.closeTableSessionHandler =
        Objects.requireNonNull(
            closeTableSessionHandler, "closeTableSessionHandler must not be null");
  }

  @Override
  public OpenTableSessionResult open(OpenTableSessionInput input) {
    return openTableSessionHandler.handle(input);
  }

  @Override
  public GetTableSessionResult getById(GetTableSessionInput input) {
    return getTableSessionHandler.handle(input);
  }

  @Override
  public CloseTableSessionResult close(CloseTableSessionInput input) {
    return closeTableSessionHandler.handle(input);
  }
}
