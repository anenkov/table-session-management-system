package com.nenkov.bar.application.session.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class OpenTableSessionResultTest {

  @Test
  void happyPath() {
    OpenTableSessionResult result = new OpenTableSessionResult("S-1", "T-1");

    assertThat(result.sessionId()).isEqualTo("S-1");
    assertThat(result.tableId()).isEqualTo("T-1");
  }

  @Test
  void nullSessionId_throwsNpe() {
    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class, () -> new OpenTableSessionResult(null, "T-1"));

    assertThat(thrown.getMessage()).contains("sessionId must not be null");
  }

  @Test
  void nullTableId_throwsNpe() {
    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class, () -> new OpenTableSessionResult("S-1", null));

    assertThat(thrown.getMessage()).contains("tableId must not be null");
  }

  @Test
  void blankSessionId_throwsIae() {
    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class, () -> new OpenTableSessionResult("   ", "T-1"));

    assertThat(thrown.getMessage()).contains("sessionId must not be blank");
  }

  @Test
  void blankTableId_throwsIae() {
    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class, () -> new OpenTableSessionResult("S-1", "   "));

    assertThat(thrown.getMessage()).contains("tableId must not be blank");
  }
}
