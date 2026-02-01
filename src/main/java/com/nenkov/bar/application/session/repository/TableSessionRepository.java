package com.nenkov.bar.application.session.repository;

import com.nenkov.bar.domain.model.session.TableSession;
import com.nenkov.bar.domain.model.session.TableSessionId;
import java.util.Optional;

/**
 * Persistence-facing boundary for loading and storing {@link TableSession} aggregates.
 *
 * <p>Contract notes:
 *
 * <ul>
 *   <li>Not-found is represented via {@link Optional}.
 *   <li>Technical persistence failures are surfaced via unchecked exceptions.
 * </ul>
 */
public interface TableSessionRepository {

  /**
   * Loads a {@link TableSession} by id.
   *
   * @param sessionId aggregate id (non-null)
   * @return {@link Optional#empty()} if not found
   * @throws RepositoryAccessException on technical/persistence failures
   */
  Optional<TableSession> findById(TableSessionId sessionId);

  /**
   * Persists the given {@link TableSession}.
   *
   * <p>Semantics: upsert (create or update).
   *
   * @param session aggregate (non-null)
   * @throws RepositoryAccessException on technical/persistence failures
   */
  void save(TableSession session);
}
