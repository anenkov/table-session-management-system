package com.nenkov.bar.domain.service.payment.allocation;

import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.money.MoneyPolicy;
import com.nenkov.bar.domain.model.session.OrderItemId;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Allocates a total {@link Money} amount proportionally across items using per-item caps.
 *
 * <p>Pipeline:
 *
 * <ol>
 *   <li>Compute raw shares: {@code totalToAllocate * cap / totalCap} (high precision)
 *   <li>Round each share via {@link Money#of(String, java.math.BigDecimal)}
 *   <li>Cap each rounded share by the provided per-item cap
 *   <li>Distribute any cent-level remainder deterministically using {@link RemainderDistributor}
 * </ol>
 *
 * <p>Determinism contract:
 *
 * <ul>
 *   <li>Deterministic results require deterministic iteration order of {@code caps}.
 *   <li>This allocator preserves insertion order in its intermediate maps.
 * </ul>
 *
 * <p>Intermediate calculations use {@link MoneyPolicy#WORK_CONTEXT}. Final rounding/normalization
 * is applied only via {@link Money#of(String, java.math.BigDecimal)}.
 */
public final class ProportionalAllocator {

  private final RemainderDistributor remainderDistributor;

  private ProportionalAllocator(RemainderDistributor remainderDistributor) {
    this.remainderDistributor =
        Objects.requireNonNull(remainderDistributor, "remainderDistributor must not be null");
  }

  public static ProportionalAllocator defaultAllocator() {
    return new ProportionalAllocator(new LargestFractionalRemainderDistributor());
  }

  /**
   * Allocates {@code totalToAllocate} proportionally across items using {@code caps} as weights and
   * as per-item upper bounds for the final rounded allocations.
   *
   * <p>If {@code totalToAllocate} is zero, returns a zero allocation for every id in {@code caps}
   * (and returns an empty map when {@code caps} is empty).
   *
   * <h4>Requirements</h4>
   *
   * <ul>
   *   <li>{@code totalToAllocate} must have the same currency as all values in {@code caps}
   *   <li>{@code caps} must contain a cap for every item that participates in the allocation
   *   <li>When {@code totalToAllocate} is non-zero, at least one cap must be &gt; 0
   * </ul>
   *
   * <p>Remainders caused by cent-level rounding are distributed deterministically by the configured
   * {@link RemainderDistributor}.
   *
   * @return allocated amounts per item id (iteration order is deterministic)
   * @throws IllegalArgumentException if {@code totalToAllocate} is non-zero and the sum of caps is
   *     zero
   */
  public Map<OrderItemId, Money> allocate(
      String currency, Money totalToAllocate, Map<OrderItemId, Money> caps) {
    Objects.requireNonNull(currency, "currency must not be null");
    Objects.requireNonNull(totalToAllocate, "totalToAllocate must not be null");
    Objects.requireNonNull(caps, "caps must not be null");

    verifyCurrency(currency, totalToAllocate, caps);

    if (totalToAllocate.isZero()) {
      return zeroAllocation(currency, caps);
    }

    Money totalCap = sumMoney(currency, caps.values());
    if (totalCap.isZero()) {
      throw new IllegalArgumentException("Total cap is zero; cannot allocate proportionally");
    }

    AllocationDraft draft = draftAllocation(currency, totalToAllocate, caps, totalCap);
    if (draft.remainderAmount().signum() == 0) {
      return draft.rounded();
    }

    return remainderDistributor.distribute(
        currency, draft.remainderAmount(), caps, draft.shares(), draft.rounded());
  }

  private static void verifyCurrency(
      String currency, Money totalToAllocate, Map<OrderItemId, Money> caps) {
    if (!currency.equals(totalToAllocate.currency())) {
      throw new IllegalArgumentException("Currency mismatch for totalToAllocate");
    }
    for (Money cap : caps.values()) {
      if (!currency.equals(cap.currency())) {
        throw new IllegalArgumentException("Currency mismatch in caps");
      }
    }
  }

  /**
   * Returns a zero-valued allocation for all item ids present in {@code caps}.
   *
   * <p>The resulting map contains one entry per {@link OrderItemId} in {@code caps}, each mapped to
   * {@link Money#zero(String)} using the provided {@code currency}.
   *
   * <p>The iteration order of the returned map matches the iteration order of {@code
   * caps.keySet()}, ensuring deterministic behavior for downstream processing.
   *
   * <p>This method is used when {@code totalToAllocate} is zero, in which case proportional share
   * computation and remainder distribution are skipped entirely.
   */
  private static Map<OrderItemId, Money> zeroAllocation(
      String currency, Map<OrderItemId, Money> caps) {
    return zeros(currency, caps.keySet());
  }

  private static AllocationDraft draftAllocation(
      String currency, Money totalToAllocate, Map<OrderItemId, Money> caps, Money totalCap) {
    List<Share> shares = computeShares(totalToAllocate, caps, totalCap);
    Map<OrderItemId, Money> rounded = roundAndCap(currency, caps, shares);

    Money sumRounded = sumMoney(currency, rounded.values());

    // Signed rounding delta: totalToAllocate.amount() - sumRounded.amount().
    // May be negative if rounding pushed the per-item sum above the total.
    // Represented as BigDecimal because Money is non-negative.
    BigDecimal remainderAmount = totalToAllocate.amount().subtract(sumRounded.amount());

    return new AllocationDraft(shares, rounded, remainderAmount);
  }

  /**
   * Computes raw proportional shares for each id without rounding to cents.
   *
   * <p>Uses {@link MoneyPolicy#WORK_CONTEXT} for intermediate precision. The returned raw values
   * are later rounded via {@link Money#of(String, java.math.BigDecimal)}.
   */
  private static List<Share> computeShares(
      Money totalToAllocate, Map<OrderItemId, Money> caps, Money totalCap) {
    List<Share> shares = new ArrayList<>();

    for (Map.Entry<OrderItemId, Money> e : caps.entrySet()) {
      OrderItemId id = e.getKey();
      Money cap = e.getValue();

      BigDecimal raw =
          totalToAllocate
              .amount()
              .multiply(cap.amount())
              .divide(totalCap.amount(), MoneyPolicy.WORK_CONTEXT);

      shares.add(new Share(id, raw));
    }

    return shares;
  }

  /**
   * Rounds raw shares to {@link Money} and caps them by the provided per-item cap.
   *
   * <p>Rounding uses {@link Money#of(String, java.math.BigDecimal)} to apply the domain rounding
   * rules. Capping ensures the allocation never exceeds the per-item maximum.
   */
  private static Map<OrderItemId, Money> roundAndCap(
      String currency, Map<OrderItemId, Money> caps, List<Share> shares) {
    Map<OrderItemId, Money> result = new LinkedHashMap<>();

    for (Share s : shares) {
      Money rounded = Money.of(currency, s.raw());
      Money cap = capOrThrow(s.id(), caps);

      Money capped = rounded.compareTo(cap) > 0 ? cap : rounded;
      result.put(s.id(), capped);
    }

    return result;
  }

  private static Money capOrThrow(OrderItemId id, Map<OrderItemId, Money> caps) {
    Money cap = caps.get(id);
    if (cap == null) {
      throw new IllegalArgumentException("Missing cap for item: " + id.value());
    }
    return cap;
  }

  private static Map<OrderItemId, Money> zeros(String currency, Iterable<OrderItemId> ids) {
    Map<OrderItemId, Money> result = new LinkedHashMap<>();
    for (OrderItemId id : ids) {
      result.put(id, Money.zero(currency));
    }
    return result;
  }

  private static Money sumMoney(String currency, Iterable<Money> monies) {
    Money total = Money.zero(currency);
    for (Money m : monies) {
      if (!currency.equals(m.currency())) {
        throw new IllegalArgumentException("Currency mismatch while summing monies");
      }
      total = total.plus(m);
    }
    return total;
  }

  public record Share(OrderItemId id, BigDecimal raw) {
    public Share {
      Objects.requireNonNull(id, "id must not be null");
      Objects.requireNonNull(raw, "raw must not be null");
    }
  }

  record AllocationDraft(
      List<Share> shares, Map<OrderItemId, Money> rounded, BigDecimal remainderAmount) {

    AllocationDraft {
      Objects.requireNonNull(shares, "shares must not be null");
      Objects.requireNonNull(rounded, "rounded must not be null");
      Objects.requireNonNull(remainderAmount, "remainderAmount must not be null");
    }
  }
}
