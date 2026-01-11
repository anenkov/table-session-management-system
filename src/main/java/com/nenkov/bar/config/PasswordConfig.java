package com.nenkov.bar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    // BCrypt: safe default for storing passwords (adaptive hashing)
    return new BCryptPasswordEncoder();
  }
}
