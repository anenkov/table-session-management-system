package com.nenkov.bar.web.api.session;

/** Response returned after successfully closing a session. */
public record CloseSessionResponse(String sessionId, String status, String closedAt) {}
