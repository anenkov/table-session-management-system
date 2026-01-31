package com.nenkov.bar.domain.model.payment;

import com.nenkov.bar.domain.exceptions.IllegalDomainStateException;
import com.nenkov.bar.domain.model.money.Money;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Represents a single payment attempt (and its result) against a session's outstanding balance.
 *
 * <p>A {@code Check} is a domain entity identified by {@link CheckId}. It is a payment snapshot:
 * the {@code amount} and {@code paidItems} are immutable after creation.
 *
 * <p>Write-offs and pricing policies are not owned by {@code Check}. They live on the session, and
 * a domain service computes the final {@code amount} and {@code paidItems} at creation time.
 *
 * <p>Invariants:
 *
 * <ul>
 *   <li>{@code id} is non-null
 *   <li>{@code amount} is non-null and strictly greater than zero
 *   <li>{@code paidItems} is non-null and non-empty
 *   <li>All {@code paidItems.paidAmount.currency} match {@code amount.currency}
 *   <li>{@code createdAt} is non-null
 *   <li>{@code completedAt} is null unless status is terminal (PAID/FAILED/CANCELED)
 * </ul>
 */
public final class Check {

  private final CheckId id;
  private final Money amount;
  private final List<PaidItem> paidItems;

  private CheckStatus status;
  private PaymentReference paymentReference;
  private final Instant createdAt;
  private Instant completedAt;

  private static final String COMPLETED_AT_ERROR_MSG = "completedAt must not be null";

  private Check(CheckId id, Money amount, List<PaidItem> paidItems, Instant createdAt) {
    this.id = Objects.requireNonNull(id, "id must not be null");
    this.amount = Objects.requireNonNull(amount, "amount must not be null");
    this.paidItems = List.copyOf(Objects.requireNonNull(paidItems, "paidItems must not be null"));
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");

    if (amount.isZero()) {
      throw new IllegalArgumentException("amount must be strictly greater than zero");
    }
    if (this.paidItems.isEmpty()) {
      throw new IllegalArgumentException("paidItems must not be empty");
    }
    validateCurrenciesMatch(amount, this.paidItems);

    this.status = CheckStatus.CREATED;
    this.paymentReference = null;
    this.completedAt = null;
  }

  /**
   * Creates a new {@code Check} with a provided id.
   *
   * @param id non-null check id
   * @param amount strictly positive total amount charged
   * @param paidItems non-empty allocation snapshot
   * @param createdAt non-null creation timestamp
   * @return a new {@code Check} in status {@link CheckStatus#CREATED}
   */
  public static Check create(
      CheckId id, Money amount, List<PaidItem> paidItems, Instant createdAt) {
    return new Check(id, amount, paidItems, createdAt);
  }

  /**
   * Creates a new {@code Check} with a generated id.
   *
   * @param amount strictly positive total amount charged
   * @param paidItems non-empty allocation snapshot
   * @param createdAt non-null creation timestamp
   * @return a new {@code Check} in status {@link CheckStatus#CREATED}
   */
  public static Check createNew(Money amount, List<PaidItem> paidItems, Instant createdAt) {
    return new Check(CheckId.random(), amount, paidItems, createdAt);
  }

  /** Returns the check id. */
  public CheckId id() {
    return id;
  }

  /** Returns the total amount charged by this check. */
  public Money amount() {
    return amount;
  }

  /** Returns the paid item allocation snapshot (immutable). */
  public List<PaidItem> paidItems() {
    return paidItems;
  }

  /** Returns the current check status. */
  public CheckStatus status() {
    return status;
  }

  /** Returns the payment reference if present (provider correlation id). */
  public PaymentReference paymentReference() {
    return paymentReference;
  }

  /** Returns the creation timestamp. */
  public Instant createdAt() {
    return createdAt;
  }

  /** Returns the completion timestamp for terminal states, otherwise {@code null}. */
  public Instant completedAt() {
    return completedAt;
  }

  /**
   * Marks this check as authorized.
   *
   * <p>Reserved for future flows. Allowed from {@link CheckStatus#CREATED} only.
   *
   * @param reference non-null payment reference
   */
  public void markAuthorized(PaymentReference reference) {
    requireStatus(CheckStatus.CREATED, "Only CREATED checks can be authorized");
    this.paymentReference = Objects.requireNonNull(reference, "reference must not be null");
    this.status = CheckStatus.AUTHORIZED;
  }

  /**
   * Marks this check as paid.
   *
   * <p>Allowed from {@link CheckStatus#CREATED} or {@link CheckStatus#AUTHORIZED}.
   *
   * @param reference non-null payment reference
   * @param completedAt non-null completion timestamp
   */
  public void markPaid(PaymentReference reference, Instant completedAt) {
    requireStatusOneOf(
        List.of(CheckStatus.CREATED, CheckStatus.AUTHORIZED),
        "Only CREATED or AUTHORIZED checks can be paid");

    this.paymentReference = Objects.requireNonNull(reference, "reference must not be null");
    this.completedAt = Objects.requireNonNull(completedAt, COMPLETED_AT_ERROR_MSG);
    this.status = CheckStatus.PAID;
  }

  /**
   * Marks this check as failed.
   *
   * <p>Allowed from {@link CheckStatus#CREATED} or {@link CheckStatus#AUTHORIZED}.
   *
   * @param completedAt non-null completion timestamp
   */
  public void markFailed(Instant completedAt) {
    requireStatusOneOf(
        List.of(CheckStatus.CREATED, CheckStatus.AUTHORIZED),
        "Only CREATED or AUTHORIZED checks can fail");

    this.completedAt = Objects.requireNonNull(completedAt, COMPLETED_AT_ERROR_MSG);
    this.status = CheckStatus.FAILED;
  }

  /**
   * Cancels this check.
   *
   * <p>Allowed from {@link CheckStatus#CREATED} or {@link CheckStatus#AUTHORIZED}.
   *
   * @param completedAt non-null cancellation timestamp
   */
  public void cancel(Instant completedAt) {
    requireStatusOneOf(
        List.of(CheckStatus.CREATED, CheckStatus.AUTHORIZED),
        "Only CREATED or AUTHORIZED checks can be canceled");

    this.completedAt = Objects.requireNonNull(completedAt, COMPLETED_AT_ERROR_MSG);
    this.status = CheckStatus.CANCELED;
  }

  private void requireStatus(CheckStatus expected, String message) {
    if (status != expected) {
      throw new IllegalDomainStateException(message + " (current: " + status + ")");
    }
  }

  private void requireStatusOneOf(List<CheckStatus> allowed, String message) {
    if (!allowed.contains(status)) {
      throw new IllegalDomainStateException(message + " (current: " + status + ")");
    }
  }

  private static void validateCurrenciesMatch(Money amount, List<PaidItem> items) {
    String currency = amount.currency();
    for (PaidItem item : items) {
      if (!currency.equals(item.paidAmount().currency())) {
        throw new IllegalArgumentException("Currency mismatch between check amount and paid items");
      }
    }
  }
}
