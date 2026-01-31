package com.nenkov.bar.domain.service.payment.allocation;

import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.session.OrderItemId;
import java.util.List;
import java.util.Map;

/** Strategy for distributing rounding remainders after proportional allocation. */
public interface RemainderDistributor {

  Map<OrderItemId, Money> distribute(
      String currency,
      Money remainder,
      Map<OrderItemId, Money> caps,
      List<ProportionalAllocator.Share> shares,
      Map<OrderItemId, Money> current);
}
