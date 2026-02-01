package com.nenkov.bar.application.payment.repository;

import com.nenkov.bar.domain.model.payment.Check;
import com.nenkov.bar.domain.model.payment.CheckId;
import com.nenkov.bar.domain.model.session.TableSessionId;
import java.util.List;
import java.util.Optional;

/**
 * Persistence-facing boundary for storing and querying {@link Check} entities.
 *
 * <p>Contract notes:
 *
 * <ul>
 *   <li>Not-found is represented via {@link Optional}.
 *   <li>Technical persistence failures are surfaced via unchecked exceptions.
 * </ul>
 */
public interface CheckRepository {

  /**
   * Loads a {@link Check} by id.
   *
   * @param checkId check id (non-null)
   * @return {@link Optional#empty()} if not found
   * @throws RepositoryAccessException on technical/persistence failures
   */
  Optional<Check> findById(CheckId checkId);

  /**
   * Lists all checks belonging to a session.
   *
   * <p>Ordering is intentionally unspecified; if ordering becomes a use case requirement, it should
   * be made explicit (e.g., by created-at) without leaking persistence details.
   *
   * @param sessionId owning session id (non-null)
   * @return possibly empty list
   * @throws RepositoryAccessException on technical/persistence failures
   */
  List<Check> findBySessionId(TableSessionId sessionId);

  /**
   * Persists the given {@link Check}.
   *
   * <p>Semantics: upsert (create or update).
   *
   * @param check entity (non-null)
   * @throws RepositoryAccessException on technical/persistence failures
   */
  void save(Check check);
}
