package com.nenkov.bar.web.api.session;

/** Response returned after successfully opening a new table session. */
public record OpenSessionResponse(String sessionId, String tableId) {}
