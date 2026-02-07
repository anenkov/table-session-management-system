package com.nenkov.bar.config;

import com.nenkov.bar.application.common.config.ApplicationCurrency;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

  @Bean
  public ApplicationCurrency applicationCurrency(AppProperties props) {
    return new ApplicationCurrency(props.currency());
  }
}
