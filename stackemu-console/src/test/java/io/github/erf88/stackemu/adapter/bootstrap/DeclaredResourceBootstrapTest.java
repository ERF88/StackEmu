package io.github.erf88.stackemu.adapter.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.CreateTopicResponse;
import software.amazon.awssdk.services.sns.model.DeleteTopicRequest;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicRequest;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicResponse;
import software.amazon.awssdk.services.sns.model.ListTopicsResponse;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sns.model.SubscribeResponse;
import software.amazon.awssdk.services.sns.model.Topic;
import software.amazon.awssdk.services.sns.model.UnsubscribeRequest;
import software.amazon.awssdk.services.sns.paginators.ListSubscriptionsByTopicIterable;
import software.amazon.awssdk.services.sns.paginators.ListTopicsIterable;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

class DeclaredResourceBootstrapTest {
  @TempDir Path directory;

  @Test
  void failureUnsubscribesAttemptCreatedSubscriptionAndPreservesExistingResources()
      throws Exception {
    var s3 = mock(S3Client.class);
    var sqs = mock(SqsClient.class);
    var sns = mock(SnsClient.class);
    var topicPages = mock(ListTopicsIterable.class);
    var subscriptionPages = mock(ListSubscriptionsByTopicIterable.class);
    String topicArn = "arn:aws:sns:us-east-1:000000000000:existing-topic";
    String queueArn = "arn:aws:sqs:us-east-1:000000000000:existing-queue";
    String queueUrl = "http://127.0.0.1:4566/000000000000/existing-queue";
    String subscriptionArn = "arn:aws:sns:us-east-1:000000000000:existing-subscription";

    when(s3.listBuckets())
        .thenReturn(
            ListBucketsResponse.builder()
                .buckets(Bucket.builder().name("existing-bucket").build())
                .build());
    when(sqs.listQueues()).thenReturn(ListQueuesResponse.builder().queueUrls(queueUrl).build());
    when(sqs.getQueueUrl(any(GetQueueUrlRequest.class)))
        .thenReturn(GetQueueUrlResponse.builder().queueUrl(queueUrl).build());
    when(sqs.getQueueAttributes(any(GetQueueAttributesRequest.class)))
        .thenReturn(
            GetQueueAttributesResponse.builder()
                .attributes(Map.of(QueueAttributeName.QUEUE_ARN, queueArn))
                .build());
    when(sns.listTopicsPaginator()).thenReturn(topicPages);
    when(topicPages.stream())
        .thenReturn(
            Stream.of(
                ListTopicsResponse.builder()
                    .topics(Topic.builder().topicArn(topicArn).build())
                    .build()));
    when(sns.listSubscriptionsByTopicPaginator(any(ListSubscriptionsByTopicRequest.class)))
        .thenReturn(subscriptionPages);
    when(subscriptionPages.stream())
        .thenReturn(Stream.of(ListSubscriptionsByTopicResponse.builder().build()));
    when(sns.subscribe(any(SubscribeRequest.class)))
        .thenReturn(SubscribeResponse.builder().subscriptionArn(subscriptionArn).build());

    Path config = config("existing-bucket", "existing-queue", "existing-topic");
    var bootstrap = bootstrap(s3, sqs, sns, config);

    assertThrows(IllegalArgumentException.class, () -> bootstrap.bootstrap(null));

    verify(sns).unsubscribe(UnsubscribeRequest.builder().subscriptionArn(subscriptionArn).build());
    verify(sns, never()).createTopic(any(CreateTopicRequest.class));
    verify(sns, never()).deleteTopic(any(DeleteTopicRequest.class));
    verify(sqs, never()).deleteQueue(any(DeleteQueueRequest.class));
    verify(s3, never()).deleteBucket(any(DeleteBucketRequest.class));
  }

  @Test
  void rollbackAttemptsEveryCleanupAndPreservesOriginalBootstrapFailure() throws Exception {
    var s3 = mock(S3Client.class);
    var sqs = mock(SqsClient.class);
    var sns = mock(SnsClient.class);
    var topicPages = mock(ListTopicsIterable.class);
    var subscriptionPages = mock(ListSubscriptionsByTopicIterable.class);
    String topicArn = "arn:aws:sns:us-east-1:000000000000:new-topic";
    String queueArn = "arn:aws:sqs:us-east-1:000000000000:new-queue";
    String queueUrl = "http://127.0.0.1:4566/000000000000/new-queue";
    String subscriptionArn = "arn:aws:sns:us-east-1:000000000000:new-subscription";

    when(s3.listBuckets()).thenReturn(ListBucketsResponse.builder().build());
    when(s3.deleteBucket(any(DeleteBucketRequest.class)))
        .thenThrow(new IllegalStateException("bucket cleanup failed"));
    when(sqs.listQueues()).thenReturn(ListQueuesResponse.builder().build());
    when(sqs.createQueue(any(CreateQueueRequest.class)))
        .thenReturn(CreateQueueResponse.builder().queueUrl(queueUrl).build());
    when(sqs.getQueueUrl(any(GetQueueUrlRequest.class)))
        .thenReturn(GetQueueUrlResponse.builder().queueUrl(queueUrl).build());
    when(sqs.getQueueAttributes(any(GetQueueAttributesRequest.class)))
        .thenReturn(
            GetQueueAttributesResponse.builder()
                .attributes(Map.of(QueueAttributeName.QUEUE_ARN, queueArn))
                .build());
    when(sqs.deleteQueue(any(DeleteQueueRequest.class)))
        .thenThrow(new IllegalStateException("queue cleanup failed"));
    when(sns.listTopicsPaginator()).thenReturn(topicPages);
    when(topicPages.stream()).thenReturn(Stream.of(ListTopicsResponse.builder().build()));
    when(sns.createTopic(any(CreateTopicRequest.class)))
        .thenReturn(CreateTopicResponse.builder().topicArn(topicArn).build());
    when(sns.listSubscriptionsByTopicPaginator(any(ListSubscriptionsByTopicRequest.class)))
        .thenReturn(subscriptionPages);
    when(subscriptionPages.stream())
        .thenReturn(Stream.of(ListSubscriptionsByTopicResponse.builder().build()));
    when(sns.subscribe(any(SubscribeRequest.class)))
        .thenReturn(SubscribeResponse.builder().subscriptionArn(subscriptionArn).build());
    when(sns.unsubscribe(any(UnsubscribeRequest.class)))
        .thenThrow(new IllegalStateException("subscription cleanup failed"));
    when(sns.deleteTopic(any(DeleteTopicRequest.class)))
        .thenThrow(new IllegalStateException("topic cleanup failed"));

    Path config = config("new-bucket", "new-queue", "new-topic");
    var bootstrap = bootstrap(s3, sqs, sns, config);

    IllegalArgumentException failure =
        assertThrows(IllegalArgumentException.class, () -> bootstrap.bootstrap(null));

    assertEquals("subscription references an undeclared topic or queue", failure.getMessage());
    assertEquals(4, failure.getSuppressed().length);
    verify(sns, times(1)).createTopic(any(CreateTopicRequest.class));
    verify(sns).unsubscribe(any(UnsubscribeRequest.class));
    verify(sns).deleteTopic(any(DeleteTopicRequest.class));
    verify(sqs).deleteQueue(any(DeleteQueueRequest.class));
    verify(s3).deleteBucket(any(DeleteBucketRequest.class));
  }

  private Path config(String bucket, String queue, String topic) throws Exception {
    Path path = directory.resolve("stackemu.yaml");
    Files.writeString(
        path,
        """
            resources:
              buckets: [%s]
              queues: [%s]
              topics: [%s]
              subscriptions:
                - topic: %s
                  queue: %s
                - topic: undeclared-topic
                  queue: %s
            """
            .formatted(bucket, queue, topic, topic, queue, queue));
    return path;
  }

  private static DeclaredResourceBootstrap bootstrap(
      S3Client s3, SqsClient sqs, SnsClient sns, Path config) {
    Supplier<SnsClient> clients = () -> sns;
    return new DeclaredResourceBootstrap(
        s3, sqs, "http://127.0.0.1:4566", "us-east-1", config.toString(), "test", "test", clients);
  }
}
