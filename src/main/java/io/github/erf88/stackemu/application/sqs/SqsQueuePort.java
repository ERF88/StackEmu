package io.github.erf88.stackemu.application.sqs;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Queue-facing SQS contract. Queue names, not provider URLs, identify every operation. */
public interface SqsQueuePort {
  List<Queue> listQueues();

  Queue getQueue(String name);

  void createQueue(CreateQueue request);

  void updateQueue(
      String name,
      Settings settings,
      Map<String, String> tags,
      Optional<RedrivePolicy> redrivePolicy);

  void purgeQueue(String name);

  void deleteQueue(String name);

  record Queue(
      String name,
      String url,
      String arn,
      boolean fifo,
      long availableMessages,
      long inFlightMessages,
      long delayedMessages,
      Settings settings,
      Map<String, String> tags,
      Optional<RedrivePolicy> redrivePolicy) {
    public Queue {
      tags = Map.copyOf(tags);
      redrivePolicy = redrivePolicy == null ? Optional.empty() : redrivePolicy;
    }
  }

  record Settings(
      int visibilityTimeout,
      int retentionPeriod,
      int delaySeconds,
      int maximumMessageSize,
      int receiveWaitTime,
      boolean contentBasedDeduplication,
      boolean managedSse) {}

  record CreateQueue(String name, Settings settings, Map<String, String> tags) {
    public CreateQueue {
      tags = Map.copyOf(tags);
    }
  }

  record RedrivePolicy(String deadLetterQueueName, int maxReceiveCount) {}
}
