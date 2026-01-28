package com.nenkov.bar.auth.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String issuer, long ttlSeconds, String secret) {}
