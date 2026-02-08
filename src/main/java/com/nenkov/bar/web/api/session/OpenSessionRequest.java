package com.nenkov.bar.web.api.session;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for opening a new table session.
 *
 * <p>{@code tableId} is an application-level identifier (not a domain concept).
 */
public record OpenSessionRequest(@NotBlank String tableId) {}
