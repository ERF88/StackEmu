package io.github.erf88.stackemu.application.sns;

import java.nio.charset.StandardCharsets;

/** SNS bounds checked before an SDK call. */
public final class SnsValidation {
  private SnsValidation() {}

  public static void topicName(String name) {
    if (name == null) invalid();
    if (name.endsWith(".fifo")) {
      if (!name.matches("[A-Za-z0-9_-]{1,251}\\.fifo")) invalid();
    } else if (!name.matches("[A-Za-z0-9_-]{1,256}")) {
      invalid();
    }
  }

  public static String topicName(String name, boolean fifo) {
    if (name == null) invalid();
    String normalizedName = name;
    if (fifo && !normalizedName.endsWith(".fifo")) normalizedName += ".fifo";
    if (!fifo && normalizedName.endsWith(".fifo")) invalid();
    topicName(normalizedName);
    return normalizedName;
  }

  public static void message(String message) {
    if (message == null
        || message.isBlank()
        || message.getBytes(StandardCharsets.UTF_8).length > 262144) {
      invalid();
    }
  }

  public static void messageGroupId(String messageGroupId) {
    fifoMessageId(messageGroupId);
  }

  public static void messageDeduplicationId(String messageDeduplicationId) {
    fifoMessageId(messageDeduplicationId);
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

  private static void fifoMessageId(String value) {
    if (value == null
        || value.isBlank()
        || value.codePointCount(0, value.length()) > 128
        || value.codePoints().anyMatch(Character::isISOControl)) invalid();
  }
}
