package io.github.erf88.stackemu.application.sqs;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Explicit message operations. Receipt handles remain request-scoped and are never durable state.
 */
public interface SqsMessagePort {
  record OutboundMessage(
      String body,
      Map<String, String> attributes,
      Optional<String> groupId,
      Optional<String> deduplicationId) {
    public OutboundMessage {
      attributes = Map.copyOf(attributes);
      groupId = groupId == null ? Optional.empty() : groupId;
      deduplicationId = deduplicationId == null ? Optional.empty() : deduplicationId;
    }
  }

  record ReceivedMessage(
      String messageId, String receiptHandle, String body, Map<String, String> attributes) {
    public ReceivedMessage {
      attributes = Map.copyOf(attributes);
    }
  }

  void send(String queueName, OutboundMessage message);

  BatchResult sendBatch(String queueName, List<OutboundMessage> messages);

  List<ReceivedMessage> receive(String queueName, int maximumMessages, int visibilityTimeout);

  void delete(String queueName, String receiptHandle);

  BatchResult deleteBatch(String queueName, List<String> receiptHandles);

  record BatchResult(List<String> succeededIds, Map<String, String> failures) {
    public BatchResult {
      succeededIds = List.copyOf(succeededIds);
      failures = Map.copyOf(failures);
    }
  }
}
