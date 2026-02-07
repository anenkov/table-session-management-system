package com.nenkov.bar.application.session.repository;

import com.nenkov.bar.application.common.persistence.RepositoryAccessException;
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
   * Checks whether there is an OPEN session for the given application-level table id.
   *
   * <p>Note: TableId is not yet part of the domain model, so this query is application-driven.
   *
   * @param tableId application-level table id (non-null, non-blank)
   * @return true if an OPEN session exists for the table
   * @throws RepositoryAccessException on technical/persistence failures
   */
  boolean existsOpenByTableId(String tableId);

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
