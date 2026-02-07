package com.nenkov.bar.application.payment.repository;

import com.nenkov.bar.application.common.persistence.RepositoryAccessException;
import com.nenkov.bar.application.payment.model.PaymentRequestId;
import com.nenkov.bar.application.payment.model.RecordedPaymentAttempt;
import java.util.Optional;

/**
 * Persistence-facing boundary for idempotency of payment initiation requests.
 *
 * <p>Stores the outcome of the first processed request for a given {@link PaymentRequestId}.
 * Subsequent calls with the same request id can be served deterministically without re-contacting
 * the payment provider.
 */
public interface PaymentAttemptRepository {

  /**
   * Loads the stored idempotency record for the given request id.
   *
   * @param requestId idempotency key (non-null)
   * @return empty if not found
   * @throws RepositoryAccessException on technical/persistence failures
   */
  Optional<RecordedPaymentAttempt> findByRequestId(PaymentRequestId requestId);

  /**
   * Persists the idempotency record.
   *
   * <p>Semantics: insert-if-absent. Implementations should treat duplicate saves for the same
   * request id as a no-op, or return the existing record. The application handler already guards
   * the primary idempotency flow; this contract exists for persistence safety.
   *
   * @param paymentAttempt record to store (non-null)
   * @throws RepositoryAccessException on technical/persistence failures
   */
  void save(RecordedPaymentAttempt paymentAttempt);
}
