package com.nenkov.bar.application.session.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class OpenTableSessionInputTest {

  @Test
  void happyPath() {
    OpenTableSessionInput input = new OpenTableSessionInput("T-1");

    assertThat(input.tableId()).isEqualTo("T-1");
  }

  @Test
  void nullTableId_throwsNpe() {
    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class, () -> new OpenTableSessionInput(null));

    assertThat(thrown.getMessage()).contains("tableId must not be null");
  }

  @Test
  void blankTableId_throwsIae() {
    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class, () -> new OpenTableSessionInput("   "));

    assertThat(thrown.getMessage()).contains("tableId must not be blank");
  }
}
