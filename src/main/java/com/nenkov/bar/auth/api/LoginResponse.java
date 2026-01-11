package com.nenkov.bar.auth.api;

public record LoginResponse(
    String accessToken,
    long expiresInSeconds
) {}
