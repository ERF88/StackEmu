package io.github.erf88.stackemu.adapter.aws.sqs;

import io.github.erf88.stackemu.application.sqs.SqsException;
import io.github.erf88.stackemu.application.sqs.SqsQueuePort;
import io.github.erf88.stackemu.application.sqs.SqsValidation;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.ListQueueTagsRequest;
import software.amazon.awssdk.services.sqs.model.ListQueuesRequest;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.TagQueueRequest;
import software.amazon.awssdk.services.sqs.model.UntagQueueRequest;

@ApplicationScoped
public class AwsSqsQueueAdapter implements SqsQueuePort {
  private final SqsClient client;

  public AwsSqsQueueAdapter(SqsClient client) {
    this.client = client;
  }

  @Override
  public List<Queue> listQueues() {
    try {
      return client.listQueues(ListQueuesRequest.builder().build()).queueUrls().stream()
          .map(this::fromUrl)
          .sorted(java.util.Comparator.comparing(Queue::name))
          .toList();
    } catch (RuntimeException failure) {
      throw translate(failure);
    }
  }

  @Override
  public Queue getQueue(String name) {
    SqsValidation.queueName(name);
    try {
      return fromUrl(queueUrl(name));
    } catch (RuntimeException failure) {
      throw translate(failure);
    }
  }

  @Override
  public void createQueue(CreateQueue request) {
    SqsValidation.queueName(request.name());
    boolean fifo = request.name().endsWith(".fifo");
    try {
      client.createQueue(
          CreateQueueRequest.builder()
              .queueName(request.name())
              .attributesWithStrings(attributes(request.settings(), fifo))
              .tags(request.tags())
              .build());
    } catch (RuntimeException failure) {
      throw translate(failure);
    }
  }

  @Override
  public void updateQueue(
      String name,
      Settings settings,
      Map<String, String> tags,
      Optional<RedrivePolicy> redrivePolicy) {
    SqsValidation.queueName(name);
    try {
      String url = queueUrl(name);
      Map<String, String> updated = new HashMap<>(attributes(settings, name.endsWith(".fifo")));
      if (redrivePolicy.isPresent()) {
        RedrivePolicy policy = redrivePolicy.get();
        Queue target = getQueue(policy.deadLetterQueueName());
        if (target.fifo() != name.endsWith(".fifo") || policy.maxReceiveCount() < 1)
          throw new SqsException(SqsException.Kind.INVALID);
        updated.put(
            "RedrivePolicy",
            "{\"deadLetterTargetArn\":\""
                + target.arn()
                + "\",\"maxReceiveCount\":\""
                + policy.maxReceiveCount()
                + "\"}");
      } else {
        updated.put("RedrivePolicy", "");
      }
      client.setQueueAttributes(
          SetQueueAttributesRequest.builder().queueUrl(url).attributesWithStrings(updated).build());
      Map<String, String> currentTags =
          client.listQueueTags(ListQueueTagsRequest.builder().queueUrl(url).build()).tags();
      List<String> removedTags =
          currentTags.keySet().stream().filter(key -> !tags.containsKey(key)).toList();
      if (!removedTags.isEmpty())
        client.untagQueue(UntagQueueRequest.builder().queueUrl(url).tagKeys(removedTags).build());
      if (!tags.isEmpty())
        client.tagQueue(TagQueueRequest.builder().queueUrl(url).tags(tags).build());
    } catch (RuntimeException failure) {
      throw translate(failure);
    }
  }

  @Override
  public void purgeQueue(String name) {
    SqsValidation.queueName(name);
    try {
      client.purgeQueue(PurgeQueueRequest.builder().queueUrl(queueUrl(name)).build());
    } catch (RuntimeException failure) {
      throw translate(failure);
    }
  }

  @Override
  public void deleteQueue(String name) {
    SqsValidation.queueName(name);
    try {
      client.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl(name)).build());
    } catch (RuntimeException failure) {
      throw translate(failure);
    }
  }

  private static Map<String, String> attributes(Settings settings, boolean fifo) {
    return Map.of(
        "VisibilityTimeout", String.valueOf(settings.visibilityTimeout()),
        "MessageRetentionPeriod", String.valueOf(settings.retentionPeriod()),
        "DelaySeconds", String.valueOf(settings.delaySeconds()),
        "MaximumMessageSize", String.valueOf(settings.maximumMessageSize()),
        "ReceiveMessageWaitTimeSeconds", String.valueOf(settings.receiveWaitTime()),
        "FifoQueue", String.valueOf(fifo),
        "ContentBasedDeduplication", String.valueOf(fifo && settings.contentBasedDeduplication()),
        "SqsManagedSseEnabled", String.valueOf(settings.managedSse()));
  }

  private Queue fromUrl(String url) {
    var attributes =
        client
            .getQueueAttributes(
                GetQueueAttributesRequest.builder()
                    .queueUrl(url)
                    .attributeNames(QueueAttributeName.ALL)
                    .build())
            .attributesAsStrings();
    Map<String, String> tags =
        client.listQueueTags(ListQueueTagsRequest.builder().queueUrl(url).build()).tags();
    String name = url.substring(url.lastIndexOf('/') + 1);
    var settings =
        new Settings(
            integer(attributes, "VisibilityTimeout"),
            integer(attributes, "MessageRetentionPeriod"),
            integer(attributes, "DelaySeconds"),
            integer(attributes, "MaximumMessageSize"),
            integer(attributes, "ReceiveMessageWaitTimeSeconds"),
            Boolean.parseBoolean(attributes.get("ContentBasedDeduplication")),
            Boolean.parseBoolean(attributes.get("SqsManagedSseEnabled"))
                || attributes.containsKey("KmsMasterKeyId"));
    return new Queue(
        name,
        url,
        attributes.getOrDefault("QueueArn", ""),
        name.endsWith(".fifo"),
        number(attributes, "ApproximateNumberOfMessages"),
        number(attributes, "ApproximateNumberOfMessagesNotVisible"),
        number(attributes, "ApproximateNumberOfMessagesDelayed"),
        settings,
        tags,
        redrivePolicy(attributes));
  }

  private String queueUrl(String name) {
    return client.getQueueUrl(request -> request.queueName(name)).queueUrl();
  }

  private static int integer(Map<String, String> values, String key) {
    return Math.toIntExact(number(values, key));
  }

  private static long number(Map<String, String> values, String key) {
    try {
      return Long.parseLong(values.getOrDefault(key, "0"));
    } catch (NumberFormatException ignored) {
      return 0;
    }
  }

  private static Optional<RedrivePolicy> redrivePolicy(Map<String, String> attributes) {
    String value = attributes.get("RedrivePolicy");
    if (value == null) return Optional.empty();
    String arn = capture(value, "deadLetterTargetArn");
    String count = capture(value, "maxReceiveCount");
    if (arn == null || count == null) return Optional.empty();
    try {
      return Optional.of(
          new RedrivePolicy(arn.substring(arn.lastIndexOf(':') + 1), Integer.parseInt(count)));
    } catch (IllegalArgumentException ignored) {
      return Optional.empty();
    }
  }

  private static String capture(String value, String key) {
    var matcher =
        java.util.regex.Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
            .matcher(value);
    return matcher.find() ? matcher.group(1) : null;
  }

  static SqsException translate(RuntimeException failure) {
    if (failure instanceof SqsException exception) return exception;
    if (failure instanceof SdkClientException)
      return new SqsException(SqsException.Kind.UNAVAILABLE);
    if (failure instanceof QueueDoesNotExistException)
      return new SqsException(SqsException.Kind.NOT_FOUND);
    if (failure instanceof AwsServiceException service) {
      String errorCode =
          service.awsErrorDetails() == null ? "" : service.awsErrorDetails().errorCode();
      if ("AWS.SimpleQueueService.NonExistentQueue".equals(errorCode)
          || "QueueDoesNotExist".equals(errorCode)) {
        return new SqsException(SqsException.Kind.NOT_FOUND);
      }
      return switch (service.statusCode()) {
        case 400 -> new SqsException(SqsException.Kind.INVALID);
        case 403 -> new SqsException(SqsException.Kind.FORBIDDEN);
        case 404 -> new SqsException(SqsException.Kind.NOT_FOUND);
        case 409 -> new SqsException(SqsException.Kind.CONFLICT);
        case 429, 500, 502, 503, 504 -> new SqsException(SqsException.Kind.UNAVAILABLE);
        default -> new SqsException(SqsException.Kind.FAILED);
      };
    }
    return new SqsException(SqsException.Kind.FAILED);
  }
}
