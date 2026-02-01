package com.nenkov.bar.domain.service.payment.allocation;

import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.session.OrderItemId;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Strategy for distributing rounding remainders after proportional allocation.
 *
 * <p>The remainder is expressed as a signed monetary amount (scale=2), not as {@link Money},
 * because the domain forbids negative {@code Money} values.
 */
public interface RemainderDistributor {

  Map<OrderItemId, Money> distribute(
      String currency,
      BigDecimal remainderAmount,
      Map<OrderItemId, Money> caps,
      List<ProportionalAllocator.Share> shares,
      Map<OrderItemId, Money> current);
}
