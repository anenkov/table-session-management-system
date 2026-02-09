package com.nenkov.bar.web.api.common;

import org.springframework.web.server.ServerWebExchange;

/**
 * Strategy for mapping a specific exception type to an {@link ApiProblemCode} + safe response
 * detail.
 *
 * <p>This mapper must be deterministic and must not leak internal exception messages.
 *
 * @param <E> exception type handled by this mapper
 */
public interface ApiExceptionMapper<E extends Throwable> {

  /** Exact exception type handled by this mapper (used for deterministic, exact-match lookup). */
  Class<E> type();

  /** Stable API problem code to use for this exception. */
  ApiProblemCode code();

  /** Safe, client-facing detail message. Must not leak internal exception messages. */
  String safeDetail(E exception, ServerWebExchange exchange);
}
