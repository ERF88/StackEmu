package io.github.erf88.stackemu.application.sns;

import java.nio.charset.StandardCharsets;

/** SNS bounds checked before an SDK call. */
public final class SnsValidation {
  private SnsValidation() {}

  public static void topicName(String name) {
    if (name == null || name.length() > 256 || !name.matches("[A-Za-z0-9_-]{1,256}(\\.fifo)?"))
      invalid();
  }

  public static void message(String message) {
    if (message == null
        || message.isBlank()
        || message.getBytes(StandardCharsets.UTF_8).length > 262144) {
      invalid();
    }
  }

  public static void queueName(String name) {
    try {
      io.github.erf88.stackemu.application.sqs.SqsValidation.queueName(name);
    } catch (io.github.erf88.stackemu.application.sqs.SqsException failure) {
      invalid();
    }
  }

  public static void subscriptionArn(String arn) {
    if (arn == null || !arn.matches("arn:[^:]+:sns:[^:]+:[0-9]{12}:[^:]+:[0-9a-f-]+")) invalid();
  }

  private static void invalid() {
    throw new SnsException(SnsException.Kind.INVALID);
  }
}
