package com.nenkov.bar.config;

import com.nenkov.bar.application.common.persistence.RepositoryAccessException;
import com.nenkov.bar.application.session.repository.TableSessionRepository;
import com.nenkov.bar.domain.model.session.TableSession;
import com.nenkov.bar.domain.model.session.TableSessionId;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Temporary placeholder wiring for repositories not implemented yet.
 *
 * <p>Purpose: allow the Spring ApplicationContext to boot while persistence is not implemented.
 *
 * <p>Removal policy: this configuration MUST be deleted once a real {@link TableSessionRepository}
 * implementation is introduced (Phase 3.4).
 */
@Configuration
public class PlaceholderRepositoryConfig {

  @Bean
  public TableSessionRepository tableSessionRepository() {
    return new FailingTableSessionRepository();
  }

  /**
   * Fail-fast placeholder implementation of {@link TableSessionRepository}.
   *
   * <p>This implementation exists strictly for wiring purposes. Any invocation indicates an illegal
   * runtime path and will fail immediately.
   */
  private static final class FailingTableSessionRepository implements TableSessionRepository {

    private static RepositoryAccessException notImplemented() {
      return new RepositoryAccessException(
          "TableSessionRepository persistence is not implemented yet.");
    }

    @Override
    public Optional<TableSession> findById(TableSessionId sessionId) {
      throw notImplemented();
    }

    @Override
    public boolean existsOpenByTableId(String tableId) {
      throw notImplemented();
    }

    @Override
    public void save(TableSession session) {
      throw notImplemented();
    }
  }
}
