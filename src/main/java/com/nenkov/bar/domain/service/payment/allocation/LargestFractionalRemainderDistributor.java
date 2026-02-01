package com.nenkov.bar.domain.service.payment.allocation;

import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.money.MoneyPolicy;
import com.nenkov.bar.domain.model.session.OrderItemId;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Remainder distribution policy: largest rounding error first (the largest fractional remainder).
 *
 * <p>This distributor applies cent-level adjustments (±0.01) to an existing rounded allocation
 * until the provided remainder amount is fully distributed.
 *
 * <p>Determinism:
 *
 * <ul>
 *   <li>Primary ordering is by rounding error (see {@link #rank(String, List)}).
 *   <li>Ties are broken by {@code OrderItemId.value().toString()}.
 * </ul>
 *
 * <p>Mutation: this implementation updates {@code current} in-place and returns the same map
 * instance for convenience.
 */
public final class LargestFractionalRemainderDistributor implements RemainderDistributor {

  private static final Comparator<OrderItemId> TIE_BREAKER =
      Comparator.comparing(id -> id.value().toString());

  @Override
  public Map<OrderItemId, Money> distribute(
      String currency,
      BigDecimal remainderAmount,
      Map<OrderItemId, Money> caps,
      List<ProportionalAllocator.Share> shares,
      Map<OrderItemId, Money> current) {
    Objects.requireNonNull(currency, "currency must not be null");
    Objects.requireNonNull(remainderAmount, "remainderAmount must not be null");
    Objects.requireNonNull(caps, "caps must not be null");
    Objects.requireNonNull(shares, "shares must not be null");
    Objects.requireNonNull(current, "current must not be null");

    Money oneCent = Money.of(currency, MoneyPolicy.ONE_CENT);
    List<RankedShare> ranked = rank(currency, shares);

    int sign = remainderAmount.signum();
    if (sign > 0) {
      return distributePositiveRemainder(remainderAmount, oneCent, caps, ranked, current);
    }
    if (sign < 0) {
      return distributeNegativeRemainder(remainderAmount, oneCent, ranked, current);
    }
    return current;
  }

  /**
   * Ranks shares by rounding error {@code raw - rounded}.
   *
   * <p>For positive remainder distribution (adding cents), the best candidates are those with the
   * largest positive rounding error (they were rounded down the most).
   *
   * <p>For negative remainder distribution (removing cents), the best candidates are those with the
   * most negative rounding error (they were rounded up the most). The negative path re-sorts the
   * ranked list accordingly.
   */
  private static List<RankedShare> rank(String currency, List<ProportionalAllocator.Share> shares) {
    List<RankedShare> ranked = new ArrayList<>(shares.size());

    for (ProportionalAllocator.Share s : shares) {
      Money rounded = Money.of(currency, s.raw());
      BigDecimal roundingError = s.raw().subtract(rounded.amount());
      ranked.add(new RankedShare(s.id(), roundingError));
    }

    // Descending rounding error, then deterministic tie-break.
    ranked.sort(
        Comparator.comparing(RankedShare::roundingError)
            .reversed()
            .thenComparing(RankedShare::id, TIE_BREAKER));

    return ranked;
  }

  private static Map<OrderItemId, Money> distributePositiveRemainder(
      BigDecimal remainderAmount,
      Money oneCent,
      Map<OrderItemId, Money> caps,
      List<RankedShare> rankedDesc,
      Map<OrderItemId, Money> current) {

    BigDecimal left = remainderAmount;

    while (left.signum() > 0) {
      boolean progressed = applyOnePositivePass(oneCent, caps, rankedDesc, current);
      if (!progressed) {
        throw new IllegalArgumentException(
            "Unable to distribute positive rounding remainder safely");
      }
      left = left.subtract(MoneyPolicy.ONE_CENT);
    }

    return current;
  }

  private static boolean applyOnePositivePass(
      Money oneCent,
      Map<OrderItemId, Money> caps,
      List<RankedShare> rankedDesc,
      Map<OrderItemId, Money> current) {

    for (RankedShare rs : rankedDesc) {
      Money now = requireCurrent(current, rs.id());
      Money cap = requireCap(caps, rs.id());

      if (now.plus(oneCent).compareTo(cap) <= 0) {
        current.put(rs.id(), now.plus(oneCent));
        return true;
      }
    }
    return false;
  }

  private static Map<OrderItemId, Money> distributeNegativeRemainder(
      BigDecimal remainderAmount,
      Money oneCent,
      List<RankedShare> rankedDesc,
      Map<OrderItemId, Money> current) {

    // For the negative remainder, we want the most negative rounding errors first.
    List<RankedShare> rankedAsc = new ArrayList<>(rankedDesc);
    rankedAsc.sort(
        Comparator.comparing(RankedShare::roundingError)
            .thenComparing(RankedShare::id, TIE_BREAKER));

    BigDecimal left = remainderAmount;

    while (left.signum() < 0) {
      boolean progressed = applyOneNegativePass(oneCent, rankedAsc, current);
      if (!progressed) {
        throw new IllegalArgumentException(
            "Unable to distribute negative rounding remainder safely");
      }
      left = left.add(MoneyPolicy.ONE_CENT);
    }

    return current;
  }

  private static boolean applyOneNegativePass(
      Money oneCent, List<RankedShare> rankedAsc, Map<OrderItemId, Money> current) {

    for (RankedShare rs : rankedAsc) {
      Money now = requireCurrent(current, rs.id());

      if (now.compareTo(oneCent) >= 0) {
        current.put(rs.id(), now.minus(oneCent));
        return true;
      }
    }
    return false;
  }

  private static Money requireCurrent(Map<OrderItemId, Money> current, OrderItemId id) {
    Money now = current.get(id);
    if (now == null) {
      throw new IllegalArgumentException("Missing current allocation for item: " + id.value());
    }
    return now;
  }

  private static Money requireCap(Map<OrderItemId, Money> caps, OrderItemId id) {
    Money cap = caps.get(id);
    if (cap == null) {
      throw new IllegalArgumentException("Missing cap allocation for item: " + id.value());
    }
    return cap;
  }

  private record RankedShare(OrderItemId id, BigDecimal roundingError) {
    private RankedShare {
      Objects.requireNonNull(id, "id must not be null");
      Objects.requireNonNull(roundingError, "roundingError must not be null");
    }
  }
}
