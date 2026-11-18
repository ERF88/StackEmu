package io.github.erf88.stackemu.adapter.aws.sns;

import io.github.erf88.stackemu.application.sns.SnsException;
import io.github.erf88.stackemu.application.sns.SnsTopicPort;
import io.github.erf88.stackemu.application.sns.SnsValidation;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.DeleteTopicRequest;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicRequest;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

/** AWS SDK adapter for the SNS operations exposed by StackEmu v1. */
@ApplicationScoped
public class AwsSnsTopicAdapter implements SnsTopicPort {
  private final SnsClient sns;
  private final SqsClient sqs;

  public AwsSnsTopicAdapter(
      SqsClient sqs,
      @ConfigProperty(name = "stackemu.endpoint") String endpoint,
      @ConfigProperty(name = "quarkus.s3.aws.region") String region,
      @ConfigProperty(name = "quarkus.s3.aws.credentials.static-provider.access-key-id")
          String accessKey,
      @ConfigProperty(name = "quarkus.s3.aws.credentials.static-provider.secret-access-key")
          String secretKey) {
    this.sqs = sqs;
    this.sns =
        SnsClient.builder()
            .endpointOverride(URI.create(endpoint))
            .region(Region.of(region))
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
            .httpClientBuilder(UrlConnectionHttpClient.builder())
            .build();
  }

  @Override
  public List<SnsTopicPort.Topic> listTopics() {
    try {
      return sns.listTopicsPaginator().stream()
          .flatMap(page -> page.topics().stream())
          .map(this::fromSdkTopic)
          .sorted(java.util.Comparator.comparing(SnsTopicPort.Topic::name))
          .toList();
    } catch (RuntimeException failure) {
      throw translate(failure);
    }
  }

  @Override
  public SnsTopicPort.Topic createTopic(String name, boolean fifo) {
    String normalizedName = SnsValidation.topicName(name, fifo);
    try {
      var request = CreateTopicRequest.builder().name(normalizedName);
      if (fifo) request.attributes(Map.of("FifoTopic", "true"));
      return topic(sns.createTopic(request.build()).topicArn());
    } catch (RuntimeException failure) {
      throw translate(failure);
    }
  }

  @Override
  public void deleteTopic(String name) {
    SnsValidation.topicName(name);
    try {
      SnsTopicPort.Topic topic = requiredTopic(name);
      sns.deleteTopic(DeleteTopicRequest.builder().topicArn(topic.arn()).build());
    } catch (RuntimeException failure) {
      throw translate(failure);
    }
  }

  @Override
  public void publish(
      String topicName, String message, String messageGroupId, String messageDeduplicationId) {
    SnsValidation.topicName(topicName);
    SnsValidation.message(message);
    boolean fifo = topicName.endsWith(".fifo");
    if (fifo) {
      SnsValidation.messageGroupId(messageGroupId);
      SnsValidation.messageDeduplicationId(messageDeduplicationId);
    } else if (messageGroupId != null || messageDeduplicationId != null) {
      throw new SnsException(SnsException.Kind.INVALID);
    }
    try {
      SnsTopicPort.Topic topic = requiredTopic(topicName);
      var request = PublishRequest.builder().topicArn(topic.arn()).message(message);
      if (fifo) {
        request.messageGroupId(messageGroupId).messageDeduplicationId(messageDeduplicationId);
      }
      sns.publish(request.build());
    } catch (RuntimeException failure) {
      throw translate(failure);
    }
  }

  @Override
  public List<SnsTopicPort.Subscription> listSubscriptions(String topicName) {
    SnsValidation.topicName(topicName);
    try {
      SnsTopicPort.Topic topic = requiredTopic(topicName);
      return sns
          .listSubscriptionsByTopicPaginator(
              ListSubscriptionsByTopicRequest.builder().topicArn(topic.arn()).build())
          .stream()
          .flatMap(page -> page.subscriptions().stream())
          .map(
              item ->
                  new SnsTopicPort.Subscription(
                      item.subscriptionArn(), item.protocol(), item.endpoint()))
          .sorted(java.util.Comparator.comparing(SnsTopicPort.Subscription::endpoint))
          .toList();
    } catch (RuntimeException failure) {
      throw translate(failure);
    }
  }

  @Override
  public SnsTopicPort.Subscription subscribeQueue(String topicName, String queueName) {
    SnsValidation.topicName(topicName);
    SnsValidation.queueName(queueName);
    try {
      SnsTopicPort.Topic topic = requiredTopic(topicName);
      String queueUrl = sqs.getQueueUrl(request -> request.queueName(queueName)).queueUrl();
      String queueArn =
          sqs.getQueueAttributes(
                  GetQueueAttributesRequest.builder()
                      .queueUrl(queueUrl)
                      .attributeNames(QueueAttributeName.QUEUE_ARN)
                      .build())
              .attributesAsStrings()
              .get("QueueArn");
      if (queueArn == null || queueArn.isBlank()) throw new SnsException(SnsException.Kind.FAILED);

      List<SnsTopicPort.Subscription> current = listSubscriptions(topicName);
      for (SnsTopicPort.Subscription subscription : current) {
        if ("sqs".equals(subscription.protocol()) && queueArn.equals(subscription.endpoint())) {
          return subscription;
        }
      }

      var created =
          sns.subscribe(
              SubscribeRequest.builder()
                  .topicArn(topic.arn())
                  .protocol("sqs")
                  .endpoint(queueArn)
                  .build());
      return new SnsTopicPort.Subscription(created.subscriptionArn(), "sqs", queueArn);
    } catch (RuntimeException failure) {
      throw translate(failure);
    }
  }

  @Override
  public void unsubscribe(String topicName, String subscriptionArn) {
    SnsValidation.topicName(topicName);
    SnsValidation.subscriptionArn(subscriptionArn);
    try {
      boolean belongsToTopic =
          listSubscriptions(topicName).stream()
              .anyMatch(subscription -> subscriptionArn.equals(subscription.arn()));
      if (!belongsToTopic) throw new SnsException(SnsException.Kind.NOT_FOUND);
      sns.unsubscribe(request -> request.subscriptionArn(subscriptionArn));
    } catch (RuntimeException failure) {
      throw translate(failure);
    }
  }

  @PreDestroy
  void close() {
    sns.close();
  }

  private SnsTopicPort.Topic requiredTopic(String name) {
    return listTopics().stream()
        .filter(topic -> topic.name().equals(name))
        .findFirst()
        .orElseThrow(() -> new SnsException(SnsException.Kind.NOT_FOUND));
  }

  private SnsTopicPort.Topic fromSdkTopic(software.amazon.awssdk.services.sns.model.Topic topic) {
    return topic(topic.topicArn());
  }

  private SnsTopicPort.Topic topic(String arn) {
    return new SnsTopicPort.Topic(arn.substring(arn.lastIndexOf(':') + 1), arn);
  }

  private static SnsException translate(RuntimeException failure) {
    if (failure instanceof SnsException exception) return exception;
    if (failure instanceof SdkClientException)
      return new SnsException(SnsException.Kind.UNAVAILABLE);
    if (failure instanceof AwsServiceException service) {
      return switch (service.statusCode()) {
        case 400 -> new SnsException(SnsException.Kind.INVALID);
        case 403 -> new SnsException(SnsException.Kind.FORBIDDEN);
        case 404 -> new SnsException(SnsException.Kind.NOT_FOUND);
        case 409 -> new SnsException(SnsException.Kind.CONFLICT);
        case 429, 500, 502, 503, 504 -> new SnsException(SnsException.Kind.UNAVAILABLE);
        default -> new SnsException(SnsException.Kind.FAILED);
      };
    }
    return new SnsException(SnsException.Kind.FAILED);
  }
}
