package com.nenkov.bar.application.payment.handler;

import com.nenkov.bar.application.payment.model.CreateCheckInput;
import com.nenkov.bar.application.payment.model.CreateCheckResult;
import com.nenkov.bar.application.payment.repository.CheckRepository;
import com.nenkov.bar.application.session.exception.TableSessionNotFoundException;
import com.nenkov.bar.application.session.repository.TableSessionRepository;
import com.nenkov.bar.domain.model.payment.Check;
import com.nenkov.bar.domain.model.payment.CheckQuote;
import com.nenkov.bar.domain.model.session.TableSession;
import com.nenkov.bar.domain.service.payment.CheckAmountCalculator;
import java.time.Instant;
import java.util.Objects;

/**
 * Workflow handler: create a check by quoting the selected items from the current session payable
 * state.
 *
 * <p>Orchestrates:
 *
 * <ul>
 *   <li>load session (repository)
 *   <li>quote amount/allocation (domain service)
 *   <li>create Check (domain entity)
 *   <li>persist Check (repository)
 * </ul>
 */
public final class CreateCheckHandler {

  private final TableSessionRepository tableSessionRepository;
  private final CheckRepository checkRepository;
  private final CheckAmountCalculator checkAmountCalculator;

  public CreateCheckHandler(
      TableSessionRepository tableSessionRepository,
      CheckRepository checkRepository,
      CheckAmountCalculator checkAmountCalculator) {

    this.tableSessionRepository =
        Objects.requireNonNull(tableSessionRepository, "tableSessionRepository must not be null");
    this.checkRepository =
        Objects.requireNonNull(checkRepository, "checkRepository must not be null");
    this.checkAmountCalculator =
        Objects.requireNonNull(checkAmountCalculator, "checkAmountCalculator must not be null");
  }

  public CreateCheckResult handle(CreateCheckInput input) {
    Objects.requireNonNull(input, "input must not be null");

    TableSession session =
        tableSessionRepository
            .findById(input.sessionId())
            .orElseThrow(() -> new TableSessionNotFoundException(input.sessionId()));

    CheckQuote quote =
        checkAmountCalculator.quote(
            session.currency(),
            session.payableItemsSnapshot(),
            input.selections(),
            session.itemWriteOffs(),
            session.sessionWriteOffs());

    Check check =
        Check.createNew(session.id(), quote.checkAmount(), quote.paidItems(), Instant.now());

    checkRepository.save(check);

    return new CreateCheckResult(session.id(), check.id(), check.amount());
  }
}
