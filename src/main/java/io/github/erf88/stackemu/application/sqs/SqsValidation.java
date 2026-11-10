package io.github.erf88.stackemu.application.sqs;

import java.nio.charset.StandardCharsets;

/** SQS bounds checked before an SDK call. */
public final class SqsValidation {
  private SqsValidation() {}

  public static void queueName(String name) {
    if (name == null || !name.matches("[A-Za-z0-9_-]{1,80}(\\.fifo)?")) invalid();
  }

  public static void receiveLimits(int maximumMessages, int visibilityTimeout) {
    if (maximumMessages < 1
        || maximumMessages > 10
        || visibilityTimeout < 0
        || visibilityTimeout > 43200) invalid();
  }

  public static void body(String body) {
    if (body == null || body.getBytes(StandardCharsets.UTF_8).length > 262144) invalid();
  }

  private static void invalid() {
    throw new SqsException(SqsException.Kind.INVALID);
  }
}
