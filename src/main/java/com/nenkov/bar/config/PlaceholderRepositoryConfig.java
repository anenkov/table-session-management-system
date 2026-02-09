package com.nenkov.bar.config;

import com.nenkov.bar.application.common.persistence.RepositoryAccessException;
import com.nenkov.bar.application.payment.exception.PaymentGatewayException;
import com.nenkov.bar.application.payment.gateway.PaymentGateway;
import com.nenkov.bar.application.payment.model.PaymentAttemptResult;
import com.nenkov.bar.application.payment.model.PaymentRequestId;
import com.nenkov.bar.application.payment.model.RecordedPaymentAttempt;
import com.nenkov.bar.application.payment.repository.CheckRepository;
import com.nenkov.bar.application.payment.repository.PaymentAttemptRepository;
import com.nenkov.bar.application.session.repository.TableSessionRepository;
import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.payment.Check;
import com.nenkov.bar.domain.model.payment.CheckId;
import com.nenkov.bar.domain.model.session.TableSession;
import com.nenkov.bar.domain.model.session.TableSessionId;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Temporary placeholder wiring for repositories/gateways not implemented yet.
 *
 * <p>Purpose: allow the Spring ApplicationContext to boot while persistence/integrations are not
 * implemented.
 *
 * <p>Removal policy: this configuration MUST be deleted once real implementations are introduced
 * (Phase 3.4+).
 */
@Configuration
public class PlaceholderRepositoryConfig {

  private static final String TS_REPO_PERSISTENCE = "TableSessionRepository persistence";
  private static final String CHECK_REPO_PERSISTENCE = "CheckRepository persistence";
  private static final String PAYMENT_ATTEMPT_REPO_PERSISTENCE =
      "PaymentAttemptRepository persistence";

  @Bean
  public TableSessionRepository tableSessionRepository() {
    return new FailingTableSessionRepository();
  }

  @Bean
  public CheckRepository checkRepository() {
    return new FailingCheckRepository();
  }

  @Bean
  public PaymentAttemptRepository paymentAttemptRepository() {
    return new FailingPaymentAttemptRepository();
  }

  @Bean
  public PaymentGateway paymentGateway() {
    return new FailingPaymentGateway();
  }

  private static RepositoryAccessException notImplemented(String component) {
    return new RepositoryAccessException(component + " is not implemented yet.");
  }

  private static final class FailingTableSessionRepository implements TableSessionRepository {

    @Override
    public Optional<TableSession> findById(TableSessionId sessionId) {
      throw notImplemented(TS_REPO_PERSISTENCE);
    }

    @Override
    public boolean existsOpenByTableId(String tableId) {
      throw notImplemented(TS_REPO_PERSISTENCE);
    }

    @Override
    public void save(TableSession session) {
      throw notImplemented(TS_REPO_PERSISTENCE);
    }
  }

  private static final class FailingCheckRepository implements CheckRepository {

    @Override
    public Optional<Check> findById(CheckId checkId) {
      throw notImplemented(CHECK_REPO_PERSISTENCE);
    }

    @Override
    public List<Check> findBySessionId(TableSessionId sessionId) {
      throw notImplemented(CHECK_REPO_PERSISTENCE);
    }

    @Override
    public void save(Check check) {
      throw notImplemented(CHECK_REPO_PERSISTENCE);
    }
  }

  private static final class FailingPaymentAttemptRepository implements PaymentAttemptRepository {

    @Override
    public Optional<RecordedPaymentAttempt> findByRequestId(PaymentRequestId requestId) {
      throw notImplemented(PAYMENT_ATTEMPT_REPO_PERSISTENCE);
    }

    @Override
    public void save(RecordedPaymentAttempt paymentAttempt) {
      throw notImplemented(PAYMENT_ATTEMPT_REPO_PERSISTENCE);
    }
  }

  private static final class FailingPaymentGateway implements PaymentGateway {

    @Override
    public PaymentAttemptResult initiatePayment(
        PaymentRequestId requestId, TableSessionId sessionId, CheckId checkId, Money amount) {
      throw new PaymentGatewayException("PaymentGateway integration is not implemented yet.");
    }
  }
}
