package com.nenkov.bar.domain.service.payment.allocation;

import static com.nenkov.bar.testsupport.TestFixtures.itemId;
import static com.nenkov.bar.testsupport.TestFixtures.money;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.session.OrderItemId;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class LargestFractionalRemainderDistributorTest {

  private static final String BGN = "BGN";

  private static final OrderItemId A = itemId("00000000-0000-0000-0000-000000000001");
  private static final OrderItemId B = itemId("00000000-0000-0000-0000-000000000002");
  private static final OrderItemId C = itemId("00000000-0000-0000-0000-000000000003");

  private final RemainderDistributor dist = new LargestFractionalRemainderDistributor();

  @Test
  void distribute_positiveRemainder_addsOneCent_toLargestRoundingError_thenTieBreaksById() {
    // We want remainder +0.01 and equal rounding errors so tie-break applies.
    //
    // raw 0.004 -> rounded 0.00 => roundingError +0.004
    // all equal => smallest id first => A receives the cent.
    List<ProportionalAllocator.Share> shares =
        List.of(
            new ProportionalAllocator.Share(A, new BigDecimal("0.004")),
            new ProportionalAllocator.Share(B, new BigDecimal("0.004")),
            new ProportionalAllocator.Share(C, new BigDecimal("0.004")));

    Map<OrderItemId, Money> caps = capsAll("1.00");
    Map<OrderItemId, Money> current = zerosABC();

    Map<OrderItemId, Money> result =
        dist.distribute(BGN, new BigDecimal("0.01"), caps, shares, current);

    assertEquals(money(BGN, "0.01"), result.get(A));
    assertEquals(money(BGN, "0.00"), result.get(B));
    assertEquals(money(BGN, "0.00"), result.get(C));
  }

  @Test
  void distribute_positiveRemainder_multipleCents_accumulatesOnFirstCandidate_untilCap() {
    // Important: the implementation re-walks the ranked list from the start for every cent.
    // With equal ranking and enough cap, the first candidate gets all cents (no round-robin).
    List<ProportionalAllocator.Share> shares =
        List.of(
            new ProportionalAllocator.Share(A, new BigDecimal("0.004")),
            new ProportionalAllocator.Share(B, new BigDecimal("0.004")),
            new ProportionalAllocator.Share(C, new BigDecimal("0.004")));

    Map<OrderItemId, Money> caps = capsAll("1.00");
    Map<OrderItemId, Money> current = zerosABC();

    Map<OrderItemId, Money> result =
        dist.distribute(BGN, new BigDecimal("0.02"), caps, shares, current);

    assertEquals(money(BGN, "0.02"), result.get(A));
    assertEquals(money(BGN, "0.00"), result.get(B));
    assertEquals(money(BGN, "0.00"), result.get(C));
  }

  @Test
  void distribute_negativeRemainder_removesOneCent_fromMostRoundedUp_thenTieBreaksById() {
    // Negative remainder means we must remove cents.
    //
    // raw 0.006 -> rounded 0.01 => roundingError -0.004 (rounded up)
    // all equal => smallest id first => remove from A.
    List<ProportionalAllocator.Share> shares =
        List.of(
            new ProportionalAllocator.Share(A, new BigDecimal("0.006")),
            new ProportionalAllocator.Share(B, new BigDecimal("0.006")),
            new ProportionalAllocator.Share(C, new BigDecimal("0.006")));

    Map<OrderItemId, Money> caps = capsAll("1.00");

    Map<OrderItemId, Money> current = new LinkedHashMap<>();
    current.put(A, money(BGN, "0.01"));
    current.put(B, money(BGN, "0.01"));
    current.put(C, money(BGN, "0.01"));

    Map<OrderItemId, Money> result =
        dist.distribute(BGN, new BigDecimal("-0.01"), caps, shares, current);

    assertEquals(money(BGN, "0.00"), result.get(A));
    assertEquals(money(BGN, "0.01"), result.get(B));
    assertEquals(money(BGN, "0.01"), result.get(C));
  }

  @Test
  void distribute_positiveRemainder_whenAllAtCap_throws() {
    // If all are already at cap, +0.01 cannot be placed anywhere => throw.
    List<ProportionalAllocator.Share> shares =
        List.of(
            new ProportionalAllocator.Share(A, new BigDecimal("0.004")),
            new ProportionalAllocator.Share(B, new BigDecimal("0.004")),
            new ProportionalAllocator.Share(C, new BigDecimal("0.004")));

    Map<OrderItemId, Money> caps = capsAll("0.00");
    Map<OrderItemId, Money> current = zerosABC();

    BigDecimal remainder = new BigDecimal("0.01");
    assertThrows(
        IllegalArgumentException.class,
        () -> dist.distribute(BGN, remainder, caps, shares, current));
  }

  @Test
  void distribute_negativeRemainder_whenCannotDecrementAnything_throws() {
    // Inconsistent state on purpose:
    // remainder is negative, but all current allocations are 0.00, so decrementing is impossible.
    List<ProportionalAllocator.Share> shares =
        List.of(
            new ProportionalAllocator.Share(A, new BigDecimal("0.006")),
            new ProportionalAllocator.Share(B, new BigDecimal("0.006")),
            new ProportionalAllocator.Share(C, new BigDecimal("0.006")));

    Map<OrderItemId, Money> caps = capsAll("1.00");
    Map<OrderItemId, Money> current = zerosABC();

    BigDecimal remainder = new BigDecimal("-0.01");

    assertThrows(
        IllegalArgumentException.class,
        () -> dist.distribute(BGN, remainder, caps, shares, current));
  }

  @Test
  void distribute_missingCap_rejected_whenNeededForProgress() {
    // Force the distributor to touch B's cap:
    // - A is blocked by cap (0.00), so it will try B next.
    // - B is missing from caps => should throw.
    List<ProportionalAllocator.Share> shares =
        List.of(
            new ProportionalAllocator.Share(A, new BigDecimal("0.004")),
            new ProportionalAllocator.Share(B, new BigDecimal("0.004")),
            new ProportionalAllocator.Share(C, new BigDecimal("0.004")));

    Map<OrderItemId, Money> caps = new LinkedHashMap<>();
    caps.put(A, money(BGN, "0.00"));
    // B intentionally missing
    caps.put(C, money(BGN, "1.00"));

    Map<OrderItemId, Money> current = zerosABC();

    BigDecimal remainder = new BigDecimal("0.01");

    assertThrows(
        IllegalArgumentException.class,
        () -> dist.distribute(BGN, remainder, caps, shares, current));
  }

  @Test
  void distribute_missingCurrent_rejected_whenNeededForProgress() {
    // Force the distributor to touch B's current allocation:
    // - A is blocked by cap (0.00), so it will try B next.
    // - B is missing from current => should throw.
    List<ProportionalAllocator.Share> shares =
        List.of(
            new ProportionalAllocator.Share(A, new BigDecimal("0.004")),
            new ProportionalAllocator.Share(B, new BigDecimal("0.004")),
            new ProportionalAllocator.Share(C, new BigDecimal("0.004")));

    Map<OrderItemId, Money> caps = new LinkedHashMap<>();
    caps.put(A, money(BGN, "0.00"));
    caps.put(B, money(BGN, "1.00"));
    caps.put(C, money(BGN, "1.00"));

    Map<OrderItemId, Money> current = new LinkedHashMap<>();
    current.put(A, money(BGN, "0.00"));
    // B intentionally missing
    current.put(C, money(BGN, "0.00"));
    BigDecimal remainder = new BigDecimal("0.01");
    assertThrows(
        IllegalArgumentException.class,
        () -> dist.distribute(BGN, remainder, caps, shares, current));
  }

  @Test
  void distribute_zeroRemainder_returnsCurrentUnchanged() {
    List<ProportionalAllocator.Share> shares =
        List.of(
            new ProportionalAllocator.Share(A, new BigDecimal("0.004")),
            new ProportionalAllocator.Share(B, new BigDecimal("0.004")),
            new ProportionalAllocator.Share(C, new BigDecimal("0.004")));

    Map<OrderItemId, Money> caps = capsAll("1.00");

    Map<OrderItemId, Money> current = new LinkedHashMap<>();
    current.put(A, money(BGN, "0.00"));
    current.put(B, money(BGN, "0.01"));
    current.put(C, money(BGN, "0.00"));

    Map<OrderItemId, Money> result = dist.distribute(BGN, BigDecimal.ZERO, caps, shares, current);

    // Should be identical mapping (and implementation currently returns the same reference)
    assertEquals(current, result);
    assertSame(current, result);
  }

  private static Map<OrderItemId, Money> capsAll(String cap) {
    Map<OrderItemId, Money> caps = new LinkedHashMap<>();
    caps.put(A, money(BGN, cap));
    caps.put(B, money(BGN, cap));
    caps.put(C, money(BGN, cap));
    return caps;
  }

  private static Map<OrderItemId, Money> zerosABC() {
    Map<OrderItemId, Money> current = new LinkedHashMap<>();
    current.put(A, money(BGN, "0.00"));
    current.put(B, money(BGN, "0.00"));
    current.put(C, money(BGN, "0.00"));
    return current;
  }
}
