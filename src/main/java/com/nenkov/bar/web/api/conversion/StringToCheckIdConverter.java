package com.nenkov.bar.web.api.conversion;

import com.nenkov.bar.domain.model.payment.CheckId;
import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Converts path-variable strings to {@link CheckId}.
 *
 * <p>Invalid values are exposed as {@code 400 Bad Request} with a stable user-facing detail message
 * used by web tests and API clients.
 */
@Component
public final class StringToCheckIdConverter implements Converter<String, CheckId> {

  @Override
  public CheckId convert(String source) {
    try {
      return CheckId.of(UUID.fromString(source));
    } catch (RuntimeException _) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid checkId.");
    }
  }
}
