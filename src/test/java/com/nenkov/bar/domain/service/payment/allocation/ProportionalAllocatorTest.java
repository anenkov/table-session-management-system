package com.nenkov.bar.domain.service.payment.allocation;

import static com.nenkov.bar.testsupport.TestFixtures.itemId;
import static com.nenkov.bar.testsupport.TestFixtures.money;
import static org.junit.jupiter.api.Assertions.*;

import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.session.OrderItemId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class ProportionalAllocatorTest {

  private static final String BGN = "BGN";

  private static final OrderItemId A = itemId("00000000-0000-0000-0000-000000000001");
  private static final OrderItemId B = itemId("00000000-0000-0000-0000-000000000002");
  private static final OrderItemId C = itemId("00000000-0000-0000-0000-000000000003");

  private final ProportionalAllocator allocator = ProportionalAllocator.defaultAllocator();

  @Test
  void allocate_totalZero_returnsZeroForEveryId() {
    Map<OrderItemId, Money> caps = new LinkedHashMap<>();
    caps.put(A, money(BGN, "10.00"));
    caps.put(B, money(BGN, "5.00"));

    Map<OrderItemId, Money> result = allocator.allocate(BGN, money(BGN, "0.00"), caps);

    assertEquals(money(BGN, "0.00"), result.get(A));
    assertEquals(money(BGN, "0.00"), result.get(B));
    assertEquals(2, result.size());
  }

  @Test
  void allocate_totalZero_withEmptyCaps_returnsEmpty() {
    Map<OrderItemId, Money> caps = new LinkedHashMap<>();
    Map<OrderItemId, Money> result = allocator.allocate(BGN, money(BGN, "0.00"), caps);
    assertEquals(0, result.size());
  }

  @Test
  void allocate_splitsProportionally_withoutRemainder() {
    Map<OrderItemId, Money> caps = new LinkedHashMap<>();
    caps.put(A, money(BGN, "1.00"));
    caps.put(B, money(BGN, "2.00"));

    Map<OrderItemId, Money> result = allocator.allocate(BGN, money(BGN, "3.00"), caps);

    assertEquals(money(BGN, "1.00"), result.get(A));
    assertEquals(money(BGN, "2.00"), result.get(B));
    assertEquals(money(BGN, "3.00"), sum(result));
  }

  @Test
  void allocate_singleItem_getsWholeAmount() {
    Map<OrderItemId, Money> caps = new LinkedHashMap<>();
    caps.put(A, money(BGN, "10.00"));

    Map<OrderItemId, Money> result = allocator.allocate(BGN, money(BGN, "3.25"), caps);

    assertEquals(1, result.size());
    assertEquals(money(BGN, "3.25"), result.get(A));
    assertEquals(money(BGN, "3.25"), sum(result));
  }

  @Test
  void allocate_twoItems_oneZeroCap_allocatesAllToNonZeroCap() {
    Map<OrderItemId, Money> caps = new LinkedHashMap<>();
    caps.put(A, money(BGN, "0.00"));
    caps.put(B, money(BGN, "10.00"));

    Map<OrderItemId, Money> result = allocator.allocate(BGN, money(BGN, "1.00"), caps);

    assertEquals(2, result.size());
    assertEquals(money(BGN, "0.00"), result.get(A));
    assertEquals(money(BGN, "1.00"), result.get(B));
    assertEquals(money(BGN, "1.00"), sum(result));
  }

  @Test
  void allocate_distributesNegativeRemainder_deterministically() {
    // total 0.02, caps 1.00 / 1.00 / 1.00
    // raw shares: 0.006666... each -> rounded 0.01 each => 0.03 a total
    // remainder = -0.01 => one cent must be removed from the most "rounded up" candidate.
    // With equal rounding error, the tie-breaker removes from the smallest id => A.
    Map<OrderItemId, Money> caps = new LinkedHashMap<>();
    caps.put(A, money(BGN, "1.00"));
    caps.put(B, money(BGN, "1.00"));
    caps.put(C, money(BGN, "1.00"));

    Map<OrderItemId, Money> result = allocator.allocate(BGN, money(BGN, "0.02"), caps);

    assertEquals(money(BGN, "0.00"), result.get(A));
    assertEquals(money(BGN, "0.01"), result.get(B));
    assertEquals(money(BGN, "0.01"), result.get(C));
    assertEquals(money(BGN, "0.02"), sum(result));
  }

  @Test
  void allocate_distributesPositiveRemainder_deterministically() {
    // total 0.01, caps 1.00 / 1.00 / 1.00
    // raw shares: 0.003333... each -> rounded 0.00 each => 0.00 a total
    // remainder = +0.01 => one cent must be added to the smallest id => A.
    Map<OrderItemId, Money> caps = new LinkedHashMap<>();
    caps.put(A, money(BGN, "1.00"));
    caps.put(B, money(BGN, "1.00"));
    caps.put(C, money(BGN, "1.00"));

    Map<OrderItemId, Money> result = allocator.allocate(BGN, money(BGN, "0.01"), caps);

    assertEquals(money(BGN, "0.01"), result.get(A));
    assertEquals(money(BGN, "0.00"), result.get(B));
    assertEquals(money(BGN, "0.00"), result.get(C));
    assertEquals(money(BGN, "0.01"), sum(result));
  }

  @Test
  void allocate_positiveRemainder_skipsItemsAtCap() {
    // total 0.02, caps: A=0.00, B=1.00, C=1.00
    // raw shares: A=0, B=0.01, C=0.01 -> no remainder needed; ensure A stays zero.
    Map<OrderItemId, Money> caps = new LinkedHashMap<>();
    caps.put(A, money(BGN, "0.00"));
    caps.put(B, money(BGN, "1.00"));
    caps.put(C, money(BGN, "1.00"));

    Map<OrderItemId, Money> result = allocator.allocate(BGN, money(BGN, "0.02"), caps);

    assertEquals(money(BGN, "0.00"), result.get(A));
    assertEquals(money(BGN, "0.01"), result.get(B));
    assertEquals(money(BGN, "0.01"), result.get(C));
    assertEquals(money(BGN, "0.02"), sum(result));
  }

  // --- Additions (recommended) ---

  @Test
  void allocate_nonZeroTotal_withEmptyCaps_rejected() {
    Map<OrderItemId, Money> caps = new LinkedHashMap<>();
    Money total = money(BGN, "0.01");
    assertThrows(IllegalArgumentException.class, () -> allocator.allocate(BGN, total, caps));
  }

  @Test
  void allocate_neverExceedsCaps_evenAfterRemainderDistribution() {
    // total 0.03 over 3 equal caps will likely allocate 0.01 each; ensure <= cap always.
    Map<OrderItemId, Money> caps = new LinkedHashMap<>();
    caps.put(A, money(BGN, "0.01"));
    caps.put(B, money(BGN, "0.01"));
    caps.put(C, money(BGN, "0.01"));

    Map<OrderItemId, Money> result = allocator.allocate(BGN, money(BGN, "0.03"), caps);

    assertEquals(money(BGN, "0.01"), result.get(A));
    assertEquals(money(BGN, "0.01"), result.get(B));
    assertEquals(money(BGN, "0.01"), result.get(C));

    for (Map.Entry<OrderItemId, Money> e : result.entrySet()) {
      assertTrue(e.getValue().compareTo(caps.get(e.getKey())) <= 0);
    }
    assertEquals(money(BGN, "0.03"), sum(result));
  }

  @Test
  void allocate_preservesInsertionOrder_ofCapsInResult() {
    Map<OrderItemId, Money> caps = new LinkedHashMap<>();
    caps.put(B, money(BGN, "2.00"));
    caps.put(A, money(BGN, "1.00"));
    caps.put(C, money(BGN, "3.00"));

    Map<OrderItemId, Money> result = allocator.allocate(BGN, money(BGN, "1.00"), caps);

    List<OrderItemId> keys = new ArrayList<>(result.keySet());
    assertEquals(List.of(B, A, C), keys);
  }

  @Test
  void allocate_currencyMismatch_totalToAllocateCurrency_rejected() {
    Map<OrderItemId, Money> caps = new LinkedHashMap<>();
    caps.put(A, money(BGN, "1.00"));

    Money total = money("EUR", "1.00");

    assertThrows(IllegalArgumentException.class, () -> allocator.allocate(BGN, total, caps));
  }

  @Test
  void allocate_nullGuards() {
    Map<OrderItemId, Money> caps = new LinkedHashMap<>();
    caps.put(A, money(BGN, "1.00"));

    Money total = money(BGN, "1.00");

    assertThrows(NullPointerException.class, () -> allocator.allocate(null, total, caps));
    assertThrows(NullPointerException.class, () -> allocator.allocate(BGN, null, caps));
    assertThrows(NullPointerException.class, () -> allocator.allocate(BGN, total, null));
  }

  @Test
  void allocate_totalCapZero_withNonZeroTotal_rejected_evenIfMultipleItems() {
    Map<OrderItemId, Money> caps = new LinkedHashMap<>();
    caps.put(A, money(BGN, "0.00"));
    caps.put(B, money(BGN, "0.00"));
    Money total = money(BGN, "0.01");
    assertThrows(IllegalArgumentException.class, () -> allocator.allocate(BGN, total, caps));
  }

  @Test
  void allocate_currencyMismatch_inCaps_rejected() {
    Map<OrderItemId, Money> caps = new LinkedHashMap<>();
    caps.put(A, money(BGN, "1.00"));
    caps.put(B, money("EUR", "1.00"));

    Money total = money(BGN, "1.00");
    assertThrows(IllegalArgumentException.class, () -> allocator.allocate(BGN, total, caps));
  }

  private static Money sum(Map<OrderItemId, Money> map) {
    Money total = money(BGN, "0.00");
    for (Money m : map.values()) {
      total = total.plus(m);
    }
    return total;
  }
}
