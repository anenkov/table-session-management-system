package com.nenkov.bar.config;

import com.nenkov.bar.application.common.config.ApplicationCurrency;
import com.nenkov.bar.application.session.handler.CloseTableSessionHandler;
import com.nenkov.bar.application.session.handler.GetTableSessionHandler;
import com.nenkov.bar.application.session.handler.OpenTableSessionHandler;
import com.nenkov.bar.application.session.repository.TableSessionRepository;
import com.nenkov.bar.application.session.service.DefaultTableSessionService;
import com.nenkov.bar.application.session.service.TableSessionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring wiring for the Session feature (application layer façade + handlers).
 *
 * <p>The application layer remains framework-free; Spring beans are assembled here.
 */
@Configuration
public class SessionFeatureConfig {

  @Bean
  public OpenTableSessionHandler openTableSessionHandler(
      TableSessionRepository tableSessionRepository, ApplicationCurrency applicationCurrency) {
    return new OpenTableSessionHandler(tableSessionRepository, applicationCurrency);
  }

  @Bean
  public GetTableSessionHandler getTableSessionHandler(
      TableSessionRepository tableSessionRepository) {
    return new GetTableSessionHandler(tableSessionRepository);
  }

  @Bean
  public CloseTableSessionHandler closeTableSessionHandler(
      TableSessionRepository tableSessionRepository) {
    return new CloseTableSessionHandler(tableSessionRepository);
  }

  @Bean
  public TableSessionService tableSessionService(
      OpenTableSessionHandler openTableSessionHandler,
      GetTableSessionHandler getTableSessionHandler,
      CloseTableSessionHandler closeTableSessionHandler) {

    return new DefaultTableSessionService(
        openTableSessionHandler, getTableSessionHandler, closeTableSessionHandler);
  }
}
