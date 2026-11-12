package io.github.erf88.stackemu.application.sns;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SnsValidationTest {
  @Test
  void acceptsStandardAndFifoTopicNamesAtTheAwsLengthLimit() {
    assertDoesNotThrow(() -> SnsValidation.topicName("events"));
    assertDoesNotThrow(() -> SnsValidation.topicName("x".repeat(256)));
    assertDoesNotThrow(() -> SnsValidation.topicName("x".repeat(251) + ".fifo"));
  }

  @Test
  void rejectsInvalidAndOverlongTopicNames() {
    assertInvalid(() -> SnsValidation.topicName("invalid name"));
    assertInvalid(() -> SnsValidation.topicName("x".repeat(257)));
    assertInvalid(() -> SnsValidation.topicName("x".repeat(252) + ".fifo"));
  }

  @Test
  void enforcesTheMessageSizeLimitInUtf8Bytes() {
    assertDoesNotThrow(() -> SnsValidation.message("é".repeat(131072)));
    assertInvalid(() -> SnsValidation.message("é".repeat(131073)));
  }

  private static void assertInvalid(org.junit.jupiter.api.function.Executable action) {
    assertThrows(SnsException.class, action);
  }
}
