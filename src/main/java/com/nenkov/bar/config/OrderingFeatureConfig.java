package com.nenkov.bar.config;

import com.nenkov.bar.application.ordering.handler.AddOrderItemsHandler;
import com.nenkov.bar.application.ordering.service.DefaultOrderingService;
import com.nenkov.bar.application.ordering.service.OrderingService;
import com.nenkov.bar.application.session.repository.TableSessionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Ordering feature wiring. */
@Configuration
public class OrderingFeatureConfig {

  @Bean
  AddOrderItemsHandler addOrderItemsHandler(TableSessionRepository tableSessionRepository) {
    return new AddOrderItemsHandler(tableSessionRepository);
  }

  @Bean
  OrderingService orderingService(AddOrderItemsHandler addOrderItemsHandler) {
    return new DefaultOrderingService(addOrderItemsHandler);
  }
}
