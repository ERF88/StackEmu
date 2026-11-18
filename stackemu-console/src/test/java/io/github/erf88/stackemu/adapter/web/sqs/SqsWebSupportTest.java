package io.github.erf88.stackemu.adapter.web.sqs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.erf88.stackemu.application.sqs.SqsException;
import org.junit.jupiter.api.Test;

class SqsWebSupportTest {
  @Test
  void mapsStableSqsFailuresToLocalizedResponseDetails() {
    assertMapping(SqsException.Kind.INVALID, 400, "sqsInvalidRequest");
    assertMapping(SqsException.Kind.FORBIDDEN, 403, "accessDenied");
    assertMapping(SqsException.Kind.NOT_FOUND, 404, "sqsQueueNotFound");
    assertMapping(SqsException.Kind.CONFLICT, 409, "sqsQueueConflict");
    assertMapping(SqsException.Kind.UNAVAILABLE, 503, "sqsUnavailable");
    assertMapping(SqsException.Kind.FAILED, 502, "operationFailed");
  }

  private static void assertMapping(
      SqsException.Kind kind, int expectedStatus, String expectedMessageKey) {
    SqsException failure = new SqsException(kind);
    assertEquals(expectedStatus, SqsWebSupport.status(failure));
    assertEquals(expectedMessageKey, SqsWebSupport.messageKey(failure));
  }
}
