package com.nenkov.bar.web.api.payment;

import jakarta.validation.constraints.NotBlank;

/** Request to record a payment attempt for an existing check. */
public record RecordPaymentAttemptRequest(@NotBlank String requestId) {}
