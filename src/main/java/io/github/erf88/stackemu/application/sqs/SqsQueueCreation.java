package io.github.erf88.stackemu.application.sqs;

import java.util.Map;
import java.util.Optional;

/** Creates a source queue and its optional DLQ without hiding confirmed partial outcomes. */
public final class SqsQueueCreation {
  private static final SqsQueuePort.Settings DEFAULT_SETTINGS =
      new SqsQueuePort.Settings(30, 345600, 0, 262144, 0, false, true);

  private final SqsQueuePort queues;

  public SqsQueueCreation(SqsQueuePort queues) {
    this.queues = queues;
  }

  public void create(String queueName) {
    queues.createQueue(new SqsQueuePort.CreateQueue(queueName, DEFAULT_SETTINGS, Map.of()));
  }

  public void createWithDeadLetterQueue(String queueName, int maxReceiveCount) {
    if (maxReceiveCount < 1 || maxReceiveCount > 1000) {
      throw new SqsException(SqsException.Kind.INVALID);
    }

    String deadLetterQueueName = deadLetterName(queueName);
    queues.createQueue(
        new SqsQueuePort.CreateQueue(deadLetterQueueName, DEFAULT_SETTINGS, Map.of()));
    try {
      queues.createQueue(new SqsQueuePort.CreateQueue(queueName, DEFAULT_SETTINGS, Map.of()));
    } catch (SqsException failure) {
      throw new SqsQueueCreationException(failure, queueName, deadLetterQueueName, false);
    }

    try {
      queues.updateQueue(
          queueName,
          DEFAULT_SETTINGS,
          Map.of(),
          Optional.of(new SqsQueuePort.RedrivePolicy(deadLetterQueueName, maxReceiveCount)));
    } catch (SqsException failure) {
      throw new SqsQueueCreationException(failure, queueName, deadLetterQueueName, true);
    }
  }

  private static String deadLetterName(String queueName) {
    if (queueName == null) {
      throw new SqsException(SqsException.Kind.INVALID);
    }
    return queueName.endsWith(".fifo")
        ? queueName.substring(0, queueName.length() - 5) + "-dlq.fifo"
        : queueName + "-dlq";
  }
}
