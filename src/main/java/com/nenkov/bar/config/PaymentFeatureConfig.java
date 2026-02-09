package com.nenkov.bar.config;

import com.nenkov.bar.application.payment.gateway.PaymentGateway;
import com.nenkov.bar.application.payment.handler.CreateCheckHandler;
import com.nenkov.bar.application.payment.handler.RecordPaymentAttemptHandler;
import com.nenkov.bar.application.payment.repository.CheckRepository;
import com.nenkov.bar.application.payment.repository.PaymentAttemptRepository;
import com.nenkov.bar.application.payment.service.DefaultPaymentService;
import com.nenkov.bar.application.payment.service.PaymentService;
import com.nenkov.bar.application.session.repository.TableSessionRepository;
import com.nenkov.bar.domain.service.payment.CheckAmountCalculator;
import com.nenkov.bar.domain.service.payment.DefaultCheckAmountCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Payment feature wiring. */
@Configuration
public class PaymentFeatureConfig {

  @Bean
  public CheckAmountCalculator checkAmountCalculator() {
    return new DefaultCheckAmountCalculator();
  }

  @Bean
  public CreateCheckHandler createCheckHandler(
      TableSessionRepository tableSessionRepository,
      CheckRepository checkRepository,
      CheckAmountCalculator checkAmountCalculator) {
    return new CreateCheckHandler(tableSessionRepository, checkRepository, checkAmountCalculator);
  }

  @Bean
  public RecordPaymentAttemptHandler recordPaymentAttemptHandler(
      PaymentGateway paymentGateway,
      CheckRepository checkRepository,
      PaymentAttemptRepository paymentAttemptRepository) {
    return new RecordPaymentAttemptHandler(
        paymentGateway, checkRepository, paymentAttemptRepository);
  }

  @Bean
  public PaymentService paymentService(
      CreateCheckHandler createCheckHandler,
      RecordPaymentAttemptHandler recordPaymentAttemptHandler) {
    return new DefaultPaymentService(createCheckHandler, recordPaymentAttemptHandler);
  }
}
