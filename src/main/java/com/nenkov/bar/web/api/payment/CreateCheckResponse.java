package com.nenkov.bar.web.api.payment;

/** Response returned after successfully creating a check. */
public record CreateCheckResponse(String sessionId, String checkId, Money amount) {

  public record Money(String amount, String currency) {}
}
