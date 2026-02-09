package com.nenkov.bar.web.api.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Registry for {@link ApiExceptionMapper} instances with deterministic, exact-type resolution.
 *
 * <p>Resolution rule: mapper is selected only by {@code exception.getClass()} exact match.
 *
 * <p>Duplicate mapper registrations for the same exception type are rejected at startup.
 */
@Component
public class ApiExceptionMapperRegistry {

  private final Map<Class<? extends Throwable>, ApiExceptionMapper<? extends Throwable>> byType;

  public ApiExceptionMapperRegistry(List<ApiExceptionMapper<? extends Throwable>> mappers) {
    Objects.requireNonNull(mappers, "mappers must not be null");

    Map<Class<? extends Throwable>, ApiExceptionMapper<? extends Throwable>> map = new HashMap<>();
    for (ApiExceptionMapper<? extends Throwable> mapper : mappers) {
      Objects.requireNonNull(mapper, "mapper must not be null");
      Class<? extends Throwable> type =
          Objects.requireNonNull(mapper.type(), "mapper.type() must not be null");

      ApiExceptionMapper<? extends Throwable> existing = map.putIfAbsent(type, mapper);
      if (existing != null) {
        throw new IllegalStateException(
            "Duplicate ApiExceptionMapper for type "
                + type.getName()
                + ": "
                + existing.getClass().getName()
                + " and "
                + mapper.getClass().getName());
      }
    }

    this.byType = Map.copyOf(map);
  }

  @SuppressWarnings("unchecked")
  public <E extends Throwable> Optional<ApiExceptionMapper<E>> findExact(E ex) {
    if (ex == null) {
      return Optional.empty();
    }
    // Safe: registry guarantees the mapper for ex.getClass() handles exactly that type.
    return Optional.ofNullable((ApiExceptionMapper<E>) byType.get(ex.getClass()));
  }
}
