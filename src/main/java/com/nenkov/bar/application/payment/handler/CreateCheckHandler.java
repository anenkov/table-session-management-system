package com.nenkov.bar.application.payment.handler;

import com.nenkov.bar.application.payment.exception.CheckCreationNotAllowedException;
import com.nenkov.bar.application.payment.exception.InvalidPaymentSelectionException;
import com.nenkov.bar.application.payment.model.CreateCheckInput;
import com.nenkov.bar.application.payment.model.CreateCheckResult;
import com.nenkov.bar.application.payment.repository.CheckRepository;
import com.nenkov.bar.application.session.exception.TableSessionNotFoundException;
import com.nenkov.bar.application.session.repository.TableSessionRepository;
import com.nenkov.bar.domain.model.payment.Check;
import com.nenkov.bar.domain.model.payment.CheckQuote;
import com.nenkov.bar.domain.model.session.TableSession;
import com.nenkov.bar.domain.model.session.TableSessionStatus;
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
 *   <li>validate lifecycle constraints (application rule)
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

    if (session.status() == TableSessionStatus.CLOSED) {
      throw new CheckCreationNotAllowedException(session.id());
    }

    CheckQuote quote;
    try {
      quote =
          checkAmountCalculator.quote(
              session.currency(),
              session.payableItemsSnapshot(),
              input.selections(),
              session.itemWriteOffs(),
              session.sessionWriteOffs());
    } catch (IllegalArgumentException _) {
      // Business-rule violation on a well-formed request (e.g. unknown itemId / over-selected qty).
      throw new InvalidPaymentSelectionException(session.id());
    }

    Check check =
        Check.createNew(session.id(), quote.checkAmount(), quote.paidItems(), Instant.now());

    checkRepository.save(check);

    return new CreateCheckResult(session.id(), check.id(), check.amount());
  }
}
