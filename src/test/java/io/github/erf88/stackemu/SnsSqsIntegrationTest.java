package io.github.erf88.stackemu;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.DeleteTopicRequest;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sns.model.UnsubscribeRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

/** Contract coverage for the v1 SNS-to-SQS delivery promise. */
@QuarkusTest
@EnabledIfEnvironmentVariable(named = "STACKEMU_INTEGRATION", matches = "true")
class SnsSqsIntegrationTest {
  @Inject SqsClient sqs;

  @ConfigProperty(name = "stackemu.endpoint")
  String endpoint;

  @ConfigProperty(name = "quarkus.s3.aws.region")
  String region;

  private String queueUrl;
  private String topicArn;
  private String subscriptionArn;

  @AfterEach
  void removeOwnResources() {
    try (SnsClient sns = sns()) {
      if (subscriptionArn != null)
        sns.unsubscribe(UnsubscribeRequest.builder().subscriptionArn(subscriptionArn).build());
      if (topicArn != null)
        sns.deleteTopic(DeleteTopicRequest.builder().topicArn(topicArn).build());
    } catch (RuntimeException ignored) {
      // Cleanup never targets resources outside this namespaced fixture.
    }
    if (queueUrl != null) {
      try {
        sqs.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build());
      } catch (RuntimeException ignored) {
        // Cleanup never targets resources outside this namespaced fixture.
      }
    }
  }

  @Test
  void publishedSnsMessageArrivesAtSubscribedSqsQueueWithinTenSeconds() throws Exception {
    String suffix = UUID.randomUUID().toString();
    String payload = "stackemu-sns-sqs-" + suffix;
    queueUrl =
        sqs.createQueue(request -> request.queueName("stackemu-sns-sqs-" + suffix)).queueUrl();
    String queueArn =
        sqs.getQueueAttributes(
                GetQueueAttributesRequest.builder()
                    .queueUrl(queueUrl)
                    .attributeNames(QueueAttributeName.QUEUE_ARN)
                    .build())
            .attributesAsStrings()
            .get("QueueArn");
    try (SnsClient sns = sns()) {
      topicArn = sns.createTopic(request -> request.name("stackemu-sns-sqs-" + suffix)).topicArn();
      subscriptionArn =
          sns.subscribe(
                  SubscribeRequest.builder()
                      .topicArn(topicArn)
                      .protocol("sqs")
                      .endpoint(queueArn)
                      .build())
              .subscriptionArn();
      sns.publish(request -> request.topicArn(topicArn).message(payload));
    }

    long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
    while (System.nanoTime() < deadline) {
      var messages =
          sqs.receiveMessage(
                  ReceiveMessageRequest.builder()
                      .queueUrl(queueUrl)
                      .maxNumberOfMessages(1)
                      .waitTimeSeconds(1)
                      .build())
              .messages();
      if (messages.stream().anyMatch(message -> message.body().contains(payload))) return;
    }
    assertTrue(false, "SNS did not deliver the published message to SQS within ten seconds");
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
}
