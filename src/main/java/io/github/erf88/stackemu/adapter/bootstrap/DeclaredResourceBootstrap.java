package io.github.erf88.stackemu.adapter.bootstrap;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

/** Provisions the declarative sandbox resources before the console accepts traffic. */
@ApplicationScoped
public final class DeclaredResourceBootstrap {
  private final S3Client s3;
  private final SqsClient sqs;
  private final String endpoint;
  private final String region;
  private final String configPath;

  public DeclaredResourceBootstrap(
      S3Client s3,
      SqsClient sqs,
      @ConfigProperty(name = "stackemu.endpoint") String endpoint,
      @ConfigProperty(name = "quarkus.s3.aws.region") String region,
      @ConfigProperty(name = "stackemu.config.path") String configPath) {
    this.s3 = s3;
    this.sqs = sqs;
    this.endpoint = endpoint;
    this.region = region;
    this.configPath = configPath;
  }

  void bootstrap(@Observes StartupEvent event) throws Exception {
    Path config = Path.of(configPath);
    if (!Files.isRegularFile(config)) return;
    Map<?, ?> resources = resources(config);
    var createdBuckets = new ArrayList<String>();
    var createdQueues = new ArrayList<String>();
    var createdTopics = new ArrayList<String>();
    try (SnsClient sns = sns()) {
      List<String> buckets = names(resources, "buckets");
      List<String> queues = names(resources, "queues");
      List<String> topics = names(resources, "topics");
      for (String bucket : buckets) {
        if (s3.listBuckets().buckets().stream().noneMatch(item -> item.name().equals(bucket))) {
          s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
          createdBuckets.add(bucket);
        }
      }
      for (String queue : queues) {
        if (sqs.listQueues().queueUrls().stream().noneMatch(url -> url.endsWith("/" + queue))) {
          createdQueues.add(
              sqs.createQueue(CreateQueueRequest.builder().queueName(queue).build()).queueUrl());
        }
      }
      for (String topic : topics) {
        if (sns.listTopics().topics().stream()
            .noneMatch(item -> item.topicArn().endsWith(":" + topic))) {
          createdTopics.add(
              sns.createTopic(CreateTopicRequest.builder().name(topic).build()).topicArn());
        }
      }
      for (Map<?, ?> subscription : subscriptions(resources)) {
        String topic = name(subscription, "topic");
        String queue = name(subscription, "queue");
        if (!topics.contains(topic) || !queues.contains(queue))
          throw new IllegalArgumentException(
              "subscription references an undeclared topic or queue");
        String topicArn =
            sns.createTopic(CreateTopicRequest.builder().name(topic).build()).topicArn();
        String queueUrl = sqs.getQueueUrl(request -> request.queueName(queue)).queueUrl();
        String queueArn =
            sqs.getQueueAttributes(
                    GetQueueAttributesRequest.builder()
                        .queueUrl(queueUrl)
                        .attributeNames(QueueAttributeName.QUEUE_ARN)
                        .build())
                .attributesAsStrings()
                .get("QueueArn");
        boolean exists =
            sns.listSubscriptionsByTopicPaginator(request -> request.topicArn(topicArn)).stream()
                .flatMap(page -> page.subscriptions().stream())
                .anyMatch(item -> queueArn.equals(item.endpoint()));
        if (!exists)
          sns.subscribe(
              SubscribeRequest.builder()
                  .topicArn(topicArn)
                  .protocol("sqs")
                  .endpoint(queueArn)
                  .build());
      }
    } catch (RuntimeException failure) {
      rollback(createdBuckets, createdQueues, createdTopics);
      throw failure;
    }
  }

  private SnsClient sns() {
    return SnsClient.builder()
        .endpointOverride(URI.create(endpoint))
        .region(Region.of(region))
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
        .httpClientBuilder(UrlConnectionHttpClient.builder())
        .build();
  }

  private void rollback(List<String> buckets, List<String> queues, List<String> topics) {
    try (SnsClient sns = sns()) {
      for (String topic : topics)
        sns.deleteTopic(DeleteTopicRequest.builder().topicArn(topic).build());
    }
    for (String queue : queues)
      sqs.deleteQueue(DeleteQueueRequest.builder().queueUrl(queue).build());
    for (String bucket : buckets)
      s3.deleteBucket(DeleteBucketRequest.builder().bucket(bucket).build());
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
