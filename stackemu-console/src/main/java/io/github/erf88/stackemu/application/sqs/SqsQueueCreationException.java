package io.github.erf88.stackemu.application.sqs;

/** Reports a failed DLQ setup while retaining the queues that were already created. */
public final class SqsQueueCreationException extends RuntimeException {
  private final SqsException failure;
  private final String queueName;
  private final String deadLetterQueueName;
  private final boolean queueCreated;

  public SqsQueueCreationException(
      SqsException failure, String queueName, String deadLetterQueueName, boolean queueCreated) {
    super(failure.kind().name(), failure);
    this.failure = failure;
    this.queueName = queueName;
    this.deadLetterQueueName = deadLetterQueueName;
    this.queueCreated = queueCreated;
  }

  public SqsException failure() {
    return failure;
  }

  public String queueName() {
    return queueName;
  }

  public String deadLetterQueueName() {
    return deadLetterQueueName;
  }

  public boolean queueCreated() {
    return queueCreated;
  }
}
