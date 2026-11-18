package io.github.erf88.stackemu.adapter.aws.sqs;

import io.github.erf88.stackemu.application.sqs.SqsMessagePort;
import io.github.erf88.stackemu.application.sqs.SqsValidation;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@ApplicationScoped
public class AwsSqsMessageAdapter implements SqsMessagePort {
  private final SqsClient client;

  public AwsSqsMessageAdapter(SqsClient client) {
    this.client = client;
  }

  @Override
  public void send(String queueName, OutboundMessage message) {
    SqsValidation.queueName(queueName);
    SqsValidation.body(message.body());
    try {
      var request =
          SendMessageRequest.builder().queueUrl(queueUrl(queueName)).messageBody(message.body());
      message.groupId().ifPresent(request::messageGroupId);
      message.deduplicationId().ifPresent(request::messageDeduplicationId);
      client.sendMessage(request.build());
    } catch (RuntimeException failure) {
      throw AwsSqsQueueAdapter.translate(failure);
    }
  }

  @Override
  public BatchResult sendBatch(String queueName, List<OutboundMessage> messages) {
    SqsValidation.queueName(queueName);
    if (messages.isEmpty() || messages.size() > 10)
      throw new io.github.erf88.stackemu.application.sqs.SqsException(
          io.github.erf88.stackemu.application.sqs.SqsException.Kind.INVALID);
    try {
      var entries = new java.util.ArrayList<SendMessageBatchRequestEntry>();
      for (int index = 0; index < messages.size(); index++) {
        var message = messages.get(index);
        SqsValidation.body(message.body());
        var entry =
            SendMessageBatchRequestEntry.builder()
                .id(String.valueOf(index))
                .messageBody(message.body());
        message.groupId().ifPresent(entry::messageGroupId);
        message.deduplicationId().ifPresent(entry::messageDeduplicationId);
        entries.add(entry.build());
      }
      var result =
          client.sendMessageBatch(
              SendMessageBatchRequest.builder()
                  .queueUrl(queueUrl(queueName))
                  .entries(entries)
                  .build());
      return new BatchResult(
          result.successful().stream().map(entry -> entry.id()).toList(),
          result.failed().stream()
              .collect(
                  java.util.stream.Collectors.toUnmodifiableMap(
                      entry -> entry.id(), entry -> entry.code())));
    } catch (RuntimeException failure) {
      throw AwsSqsQueueAdapter.translate(failure);
    }
  }

  @Override
  public List<ReceivedMessage> receive(
      String queueName, int maximumMessages, int visibilityTimeout) {
    SqsValidation.queueName(queueName);
    SqsValidation.receiveLimits(maximumMessages, visibilityTimeout);
    try {
      return client
          .receiveMessage(
              ReceiveMessageRequest.builder()
                  .queueUrl(queueUrl(queueName))
                  .maxNumberOfMessages(maximumMessages)
                  .visibilityTimeout(visibilityTimeout)
                  .messageAttributeNames("All")
                  .build())
          .messages()
          .stream()
          .map(this::fromMessage)
          .toList();
    } catch (RuntimeException failure) {
      throw AwsSqsQueueAdapter.translate(failure);
    }
  }

  @Override
  public void delete(String queueName, String receiptHandle) {
    SqsValidation.queueName(queueName);
    if (receiptHandle == null || receiptHandle.isBlank())
      throw new io.github.erf88.stackemu.application.sqs.SqsException(
          io.github.erf88.stackemu.application.sqs.SqsException.Kind.INVALID);
    try {
      client.deleteMessage(
          DeleteMessageRequest.builder()
              .queueUrl(queueUrl(queueName))
              .receiptHandle(receiptHandle)
              .build());
    } catch (RuntimeException failure) {
      throw AwsSqsQueueAdapter.translate(failure);
    }
  }

  @Override
  public BatchResult deleteBatch(String queueName, List<String> receiptHandles) {
    SqsValidation.queueName(queueName);
    if (receiptHandles.isEmpty()
        || receiptHandles.size() > 10
        || receiptHandles.stream().anyMatch(handle -> handle == null || handle.isBlank()))
      throw new io.github.erf88.stackemu.application.sqs.SqsException(
          io.github.erf88.stackemu.application.sqs.SqsException.Kind.INVALID);
    try {
      var entries = new java.util.ArrayList<DeleteMessageBatchRequestEntry>();
      for (int index = 0; index < receiptHandles.size(); index++)
        entries.add(
            DeleteMessageBatchRequestEntry.builder()
                .id(String.valueOf(index))
                .receiptHandle(receiptHandles.get(index))
                .build());
      var result =
          client.deleteMessageBatch(
              DeleteMessageBatchRequest.builder()
                  .queueUrl(queueUrl(queueName))
                  .entries(entries)
                  .build());
      return new BatchResult(
          result.successful().stream().map(entry -> entry.id()).toList(),
          result.failed().stream()
              .collect(
                  java.util.stream.Collectors.toUnmodifiableMap(
                      entry -> entry.id(), entry -> entry.code())));
    } catch (RuntimeException failure) {
      throw AwsSqsQueueAdapter.translate(failure);
    }
  }

  private String queueUrl(String name) {
    return client.getQueueUrl(request -> request.queueName(name)).queueUrl();
  }

  private ReceivedMessage fromMessage(Message message) {
    Map<String, String> attributes =
        message.messageAttributes().entrySet().stream()
            .collect(
                java.util.stream.Collectors.toUnmodifiableMap(
                    Map.Entry::getKey, entry -> entry.getValue().stringValue()));
    return new ReceivedMessage(
        message.messageId(), message.receiptHandle(), message.body(), attributes);
  }
}
