package com.nenkov.bar.application.session.service;

import com.nenkov.bar.application.session.model.CloseTableSessionInput;
import com.nenkov.bar.application.session.model.CloseTableSessionResult;
import com.nenkov.bar.application.session.model.GetTableSessionInput;
import com.nenkov.bar.application.session.model.GetTableSessionResult;
import com.nenkov.bar.application.session.model.OpenTableSessionInput;
import com.nenkov.bar.application.session.model.OpenTableSessionResult;

/**
 * Feature façade for session-related workflows.
 *
 * <p>This is the single public entry point for the session feature. It should remain thin and
 * delegate workflow logic to handlers.
 */
public interface TableSessionService {

  OpenTableSessionResult open(OpenTableSessionInput input);

  GetTableSessionResult getById(GetTableSessionInput input);

  CloseTableSessionResult close(CloseTableSessionInput input);
}
