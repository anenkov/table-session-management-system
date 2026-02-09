package com.nenkov.bar.web.api.session;

import com.nenkov.bar.application.session.model.GetTableSessionInput;
import com.nenkov.bar.application.session.model.OpenTableSessionInput;
import com.nenkov.bar.application.session.service.TableSessionService;
import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.session.TableSessionId;
import com.nenkov.bar.domain.model.writeoff.ItemWriteOff;
import com.nenkov.bar.domain.model.writeoff.WriteOff;
import com.nenkov.bar.domain.service.payment.SessionItemSnapshot;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * Session API endpoints.
 *
 * <p>Exposes:
 *
 * <ul>
 *   <li>Open a new session (enforces one-open-session-per-table)
 *   <li>Get session by {@code sessionId}
 * </ul>
 *
 * <p>Note: {@code tableId} is application-level only.
 */
@RestController
@RequestMapping(path = "/sessions", produces = MediaType.APPLICATION_JSON_VALUE)
public class SessionController {

  private final TableSessionService tableSessionService;

  public SessionController(TableSessionService tableSessionService) {
    this.tableSessionService = tableSessionService;
  }

  /**
   * Opens a new session for a table.
   *
   * <p>Conflicts are thrown by the application layer and mapped by {@code ApiExceptionHandler} to
   * RFC7807 Problem Details.
   */
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<OpenSessionResponse> open(@Valid @RequestBody OpenSessionRequest request) {
    return Mono.fromSupplier(
        () -> {
          var result = tableSessionService.open(new OpenTableSessionInput(request.tableId()));
          return new OpenSessionResponse(result.sessionId(), result.tableId());
        });
  }

  /**
   * Returns a session projection by id.
   *
   * <p>If the session does not exist, the application layer throws {@code
   * TableSessionNotFoundException} which is mapped by {@code ApiExceptionHandler} to RFC7807
   * Problem Details (404).
   */
  @GetMapping("/{sessionId}")
  public Mono<GetSessionResponse> getById(@PathVariable String sessionId) {
    return Mono.fromSupplier(
        () -> {
          TableSessionId id = parseSessionId(sessionId);
          var result = tableSessionService.getById(new GetTableSessionInput(id));

          return new GetSessionResponse(
              result.sessionId().value(),
              result.currency(),
              toPayableItems(result.payableItems()),
              toItemWriteOffs(result.itemWriteOffs()),
              toSessionWriteOffs(result.sessionWriteOffs()));
        });
  }

  private static TableSessionId parseSessionId(String raw) {
    try {
      return TableSessionId.of(raw);
    } catch (RuntimeException _) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sessionId.");
    }
  }

  private static List<GetSessionResponse.PayableItem> toPayableItems(
      List<SessionItemSnapshot> items) {
    return items.stream()
        .map(
            i ->
                new GetSessionResponse.PayableItem(
                    i.itemId().value().toString(), toMoney(i.unitPrice()), i.remainingQuantity()))
        .toList();
  }

  private static List<GetSessionResponse.ItemWriteOff> toItemWriteOffs(
      List<ItemWriteOff> writeOffs) {
    return writeOffs.stream()
        .map(
            w ->
                new GetSessionResponse.ItemWriteOff(
                    w.itemId().value().toString(),
                    w.quantity(),
                    toMoney(w.amount()),
                    w.reason().name(),
                    w.note()))
        .toList();
  }

  private static List<GetSessionResponse.SessionWriteOff> toSessionWriteOffs(
      List<WriteOff> writeOffs) {
    return writeOffs.stream()
        .map(
            w ->
                new GetSessionResponse.SessionWriteOff(
                    toMoney(w.amount()), w.reason().name(), w.note()))
        .toList();
  }

  private static GetSessionResponse.Money toMoney(Money money) {
    return new GetSessionResponse.Money(money.amount().toPlainString(), money.currency());
  }
}
