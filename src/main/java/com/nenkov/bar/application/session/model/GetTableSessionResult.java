package com.nenkov.bar.application.session.model;

import com.nenkov.bar.domain.model.session.TableSessionId;
import com.nenkov.bar.domain.model.writeoff.ItemWriteOff;
import com.nenkov.bar.domain.model.writeoff.WriteOff;
import com.nenkov.bar.domain.service.payment.SessionItemSnapshot;
import java.util.List;
import java.util.Objects;

/**
 * Result model representing the current payable state of a table session.
 *
 * <p>This is a read projection owned by the application layer.
 */
public record GetTableSessionResult(
    TableSessionId sessionId,
    String currency,
    List<SessionItemSnapshot> payableItems,
    List<ItemWriteOff> itemWriteOffs,
    List<WriteOff> sessionWriteOffs) {

  public GetTableSessionResult {
    Objects.requireNonNull(sessionId, "sessionId must not be null");
    Objects.requireNonNull(currency, "currency must not be null");
    Objects.requireNonNull(payableItems, "payableItems must not be null");
    Objects.requireNonNull(itemWriteOffs, "itemWriteOffs must not be null");
    Objects.requireNonNull(sessionWriteOffs, "sessionWriteOffs must not be null");
  }
}
