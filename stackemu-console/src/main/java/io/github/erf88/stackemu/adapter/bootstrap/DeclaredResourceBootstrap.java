package io.github.erf88.stackemu.adapter.bootstrap;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.DeleteTopicRequest;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicRequest;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sns.model.UnsubscribeRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

/** Provisions the declarative sandbox resources before the console accepts traffic. */
@ApplicationScoped
public final class DeclaredResourceBootstrap {
  private final S3Client s3;
  private final SqsClient sqs;
  private final String configPath;
  private final Supplier<SnsClient> snsClientFactory;

  @Inject
  public DeclaredResourceBootstrap(
      S3Client s3,
      SqsClient sqs,
      @ConfigProperty(name = "stackemu.endpoint") String endpoint,
      @ConfigProperty(name = "quarkus.s3.aws.region") String region,
      @ConfigProperty(name = "stackemu.config.path") String configPath,
      @ConfigProperty(name = "quarkus.s3.aws.credentials.static-provider.access-key-id")
          String accessKey,
      @ConfigProperty(name = "quarkus.s3.aws.credentials.static-provider.secret-access-key")
          String secretKey) {
    this(
        s3,
        sqs,
        endpoint,
        region,
        configPath,
        accessKey,
        secretKey,
        () ->
            SnsClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build());
  }

  DeclaredResourceBootstrap(
      S3Client s3,
      SqsClient sqs,
      String endpoint,
      String region,
      String configPath,
      String accessKey,
      String secretKey,
      Supplier<SnsClient> snsClientFactory) {
    this.s3 = s3;
    this.sqs = sqs;
    this.configPath = configPath;
    this.snsClientFactory = snsClientFactory;
  }

  void bootstrap(@Observes StartupEvent event) throws Exception {
    Path config = Path.of(configPath);
    if (!Files.isRegularFile(config)) return;
    Map<?, ?> resources = resources(config);
    var createdBuckets = new ArrayList<String>();
    var createdQueues = new ArrayList<String>();
    var createdTopics = new ArrayList<String>();
    var createdSubscriptions = new ArrayList<String>();
    var topicArns = new HashMap<String, String>();
    try (SnsClient sns = sns()) {
      List<String> buckets = names(resources, "buckets");
      List<String> queues = names(resources, "queues");
      List<String> topics = names(resources, "topics");
      sns.listTopicsPaginator().stream()
          .flatMap(page -> page.topics().stream())
          .forEach(item -> topicArns.put(topicName(item.topicArn()), item.topicArn()));
      for (String bucket : buckets) {
        if (s3.listBuckets().buckets().stream().noneMatch(item -> item.name().equals(bucket))) {
          s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
          createdBuckets.add(bucket);
        }
      }
      for (String queue : queues) {
        if (sqs.listQueues().queueUrls().stream().noneMatch(url -> url.endsWith("/" + queue))) {
          createdQueues.add(sqs.createQueue(queueRequest(queue)).queueUrl());
        }
      }
      for (String topic : topics) {
        if (!topicArns.containsKey(topic)) {
          String topicArn = sns.createTopic(topicRequest(topic)).topicArn();
          topicArns.put(topic, topicArn);
          createdTopics.add(topicArn);
        }
      }
      for (Map<?, ?> subscription : subscriptions(resources)) {
        String topic = name(subscription, "topic");
        String queue = name(subscription, "queue");
        if (!topics.contains(topic) || !queues.contains(queue))
          throw new IllegalArgumentException(
              "subscription references an undeclared topic or queue");
        String topicArn = topicArns.get(topic);
        String queueUrl =
            sqs.getQueueUrl(GetQueueUrlRequest.builder().queueName(queue).build()).queueUrl();
        String queueArn =
            sqs.getQueueAttributes(
                    GetQueueAttributesRequest.builder()
                        .queueUrl(queueUrl)
                        .attributeNames(QueueAttributeName.QUEUE_ARN)
                        .build())
                .attributesAsStrings()
                .get("QueueArn");
        boolean exists =
            sns
                .listSubscriptionsByTopicPaginator(
                    ListSubscriptionsByTopicRequest.builder().topicArn(topicArn).build())
                .stream()
                .flatMap(page -> page.subscriptions().stream())
                .anyMatch(item -> queueArn.equals(item.endpoint()));
        if (!exists) {
          String subscriptionArn =
              sns.subscribe(
                      SubscribeRequest.builder()
                          .topicArn(topicArn)
                          .protocol("sqs")
                          .endpoint(queueArn)
                          .build())
                  .subscriptionArn();
          if (subscriptionArn != null && !subscriptionArn.isBlank())
            createdSubscriptions.add(subscriptionArn);
        }
      }
    } catch (RuntimeException failure) {
      rollback(createdBuckets, createdQueues, createdTopics, createdSubscriptions, failure);
      throw failure;
    }
  }

  private SnsClient sns() {
    return snsClientFactory.get();
  }

  private static CreateQueueRequest queueRequest(String name) {
    var request = CreateQueueRequest.builder().queueName(name);
    if (name.endsWith(".fifo")) request.attributesWithStrings(Map.of("FifoQueue", "true"));
    return request.build();
  }

  private static CreateTopicRequest topicRequest(String name) {
    var request = CreateTopicRequest.builder().name(name);
    if (name.endsWith(".fifo")) request.attributes(Map.of("FifoTopic", "true"));
    return request.build();
  }

  private void rollback(
      List<String> buckets,
      List<String> queues,
      List<String> topics,
      List<String> subscriptions,
      RuntimeException failure) {
    SnsClient sns = null;
    try {
      sns = sns();
    } catch (RuntimeException cleanupFailure) {
      failure.addSuppressed(cleanupFailure);
    }
    if (sns != null) {
      SnsClient cleanupSns = sns;
      for (int index = subscriptions.size() - 1; index >= 0; index--) {
        String subscription = subscriptions.get(index);
        cleanup(
            () ->
                cleanupSns.unsubscribe(
                    UnsubscribeRequest.builder().subscriptionArn(subscription).build()),
            failure);
      }
      for (int index = topics.size() - 1; index >= 0; index--) {
        String topic = topics.get(index);
        cleanup(
            () -> cleanupSns.deleteTopic(DeleteTopicRequest.builder().topicArn(topic).build()),
            failure);
      }
      cleanup(cleanupSns::close, failure);
    }
    for (int index = queues.size() - 1; index >= 0; index--) {
      String queue = queues.get(index);
      cleanup(() -> sqs.deleteQueue(DeleteQueueRequest.builder().queueUrl(queue).build()), failure);
    }
    for (int index = buckets.size() - 1; index >= 0; index--) {
      String bucket = buckets.get(index);
      cleanup(() -> s3.deleteBucket(DeleteBucketRequest.builder().bucket(bucket).build()), failure);
    }
  }

  private static void cleanup(Runnable action, RuntimeException failure) {
    try {
      action.run();
    } catch (RuntimeException cleanupFailure) {
      failure.addSuppressed(cleanupFailure);
    }
  }

  private static String topicName(String topicArn) {
    return topicArn.substring(topicArn.lastIndexOf(':') + 1);
  }

  @SuppressWarnings("unchecked")
  private static Map<?, ?> resources(Path config) throws Exception {
    Object document =
        new Load(LoadSettings.builder().setLabel(config.toString()).build())
            .loadFromString(Files.readString(config));
    if (!(document instanceof Map<?, ?> root)
        || !(root.get("resources") instanceof Map<?, ?> values))
      throw new IllegalArgumentException("stackemu.yaml requires resources");
    return values;
  }

  private static List<String> names(Map<?, ?> resources, String key) {
    Object value = resources.get(key);
    if (value == null) return List.of();
    if (!(value instanceof List<?> values)
        || values.stream().anyMatch(item -> !(item instanceof String)))
      throw new IllegalArgumentException("resources." + key + " must be a list of names");
    return values.stream().map(String.class::cast).toList();
  }

  private static List<Map<?, ?>> subscriptions(Map<?, ?> resources) {
    Object value = resources.get("subscriptions");
    if (value == null) return List.of();
    if (!(value instanceof List<?> values)
        || values.stream().anyMatch(item -> !(item instanceof Map<?, ?>)))
      throw new IllegalArgumentException("resources.subscriptions must be a list");
    var subscriptions = new ArrayList<Map<?, ?>>();
    for (Object item : values) subscriptions.add((Map<?, ?>) item);
    return subscriptions;
  }

  private static String name(Map<?, ?> values, String key) {
    Object value = values.get(key);
    if (!(value instanceof String name) || name.isBlank())
      throw new IllegalArgumentException("subscription." + key + " is required");
    return name;
  }
}
