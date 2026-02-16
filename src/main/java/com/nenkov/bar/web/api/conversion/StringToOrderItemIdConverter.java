package com.nenkov.bar.web.api.conversion;

import com.nenkov.bar.domain.model.session.OrderItemId;
import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Converts external string values to {@link OrderItemId}.
 *
 * <p>Used for non-path HTTP inputs (for example request-body IDs) to avoid controller-local UUID
 * parsing.
 */
@Component
public final class StringToOrderItemIdConverter implements Converter<String, OrderItemId> {

  @Override
  public OrderItemId convert(String source) {
    try {
      return OrderItemId.of(UUID.fromString(source));
    } catch (RuntimeException _) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid itemId.");
    }
  }
}

