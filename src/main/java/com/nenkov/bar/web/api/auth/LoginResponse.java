package com.nenkov.bar.web.api.auth;

public record LoginResponse(String accessToken, long expiresInSeconds) {}
