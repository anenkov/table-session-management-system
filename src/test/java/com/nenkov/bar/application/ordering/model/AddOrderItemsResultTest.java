package com.nenkov.bar.application.ordering.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

final class AddOrderItemsResultTest {

  @Test
  void happyPath() {
    List<String> createdItemIds = List.of("OI-1", "OI-2");

    AddOrderItemsResult result = new AddOrderItemsResult("S-1", createdItemIds);

    assertThat(result.sessionId()).isEqualTo("S-1");
    assertThat(result.createdItemIds()).containsExactly("OI-1", "OI-2");
  }

  @Test
  void nullSessionId_throwsNpe() {
    List<String> createdItemIds = List.of("OI-1");

    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class, () -> new AddOrderItemsResult(null, createdItemIds));

    assertThat(thrown.getMessage()).contains("sessionId must not be null");
  }

  @Test
  void blankSessionId_throwsIae() {
    List<String> createdItemIds = List.of("OI-1");

    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class, () -> new AddOrderItemsResult("   ", createdItemIds));

    assertThat(thrown.getMessage()).contains("sessionId must not be blank");
  }

  @Test
  void nullCreatedItemIds_throwsNpe() {
    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class, () -> new AddOrderItemsResult("S-1", null));

    assertThat(thrown.getMessage()).contains("createdItemIds must not be null");
  }
}
