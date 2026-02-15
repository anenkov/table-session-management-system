package com.nenkov.bar.web.api.payment;

/** Response returned after recording a payment attempt. */
public record RecordPaymentAttemptResponse(
    String requestId, String sessionId, String checkId, Attempt attempt) {

  public record Attempt(String status, String providerReference, String failureReason) {}
}
