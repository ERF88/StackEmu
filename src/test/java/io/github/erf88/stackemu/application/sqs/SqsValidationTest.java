package io.github.erf88.stackemu.application.sqs;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SqsValidationTest {
  @Test
  void acceptsStandardAndFifoQueueNames() {
    assertDoesNotThrow(() -> SqsValidation.queueName("events"));
    assertDoesNotThrow(() -> SqsValidation.queueName("events.fifo"));
  }

  @Test
  void rejectsInvalidQueueNamesAndSqsLimits() {
    assertInvalid(() -> SqsValidation.queueName("invalid name"));
    assertInvalid(() -> SqsValidation.queueName("queue.fifo.extra"));
    assertInvalid(() -> SqsValidation.receiveLimits(0, 0));
    assertInvalid(() -> SqsValidation.receiveLimits(11, 0));
    assertInvalid(() -> SqsValidation.receiveLimits(1, 43201));
  }

  @Test
  void rejectsBodiesBeyondSqsLimit() {
    assertDoesNotThrow(() -> SqsValidation.body("ok"));
    assertInvalid(() -> SqsValidation.body("x".repeat(262145)));
  }

  private static void assertInvalid(org.junit.jupiter.api.function.Executable action) {
    assertThrows(SqsException.class, action);
  }
}
