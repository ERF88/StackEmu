package io.github.erf88.stackemu.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.DeleteTopicRequest;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sns.model.UnsubscribeRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

/** Direct AWS API contract coverage against the public sandbox endpoint. */
final class SandboxAwsContract {
  private static final URI ENDPOINT = URI.create("http://127.0.0.1:4566");
  private static final URI CONSOLE = URI.create("http://127.0.0.1:8080");
  private static final Region REGION = Region.US_EAST_1;
  private static final Duration MESSAGE_TIMEOUT = Duration.ofSeconds(10);

  private SandboxAwsContract() {}

  static void verifySdk(String suffix) {
    try (S3Client s3 = s3(); SqsClient sqs = sqs(); SnsClient sns = sns()) {
      verifyS3(s3, suffix);
      verifySnsAndSqs(sns, sqs, suffix);
    }
  }

  static void verifyConsoleCopy(String suffix) throws Exception {
    String sourceBucket = "console-src-" + suffix;
    String destinationBucket = "console-dst-" + suffix;
    String sourceKey = "contract-" + suffix + ".txt";
    String destinationKey = "copy-" + suffix + ".txt";
    try (S3Client s3 = s3()) {
      boolean sourceCreated = false;
      boolean destinationCreated = false;
      try {
        s3.createBucket(request -> request.bucket(sourceBucket));
        sourceCreated = true;
        s3.createBucket(request -> request.bucket(destinationBucket));
        destinationCreated = true;
        String body = "stackemu-console-contract-" + suffix;
        s3.putObject(
            PutObjectRequest.builder().bucket(sourceBucket).key(sourceKey).build(),
            RequestBody.fromString(body));

        var cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient http =
            HttpClient.newBuilder()
                .cookieHandler(cookies)
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        String form =
            get(
                http,
                "/s3/transfer?"
                    + query(
                        Map.of(
                            "bucket", sourceBucket,
                            "key", sourceKey,
                            "move", "false",
                            "destinationBucket", destinationBucket,
                            "destinationKey", destinationKey,
                            "lang", "en",
                            "theme", "dark",
                            "density", "compact")));
        String csrf = hiddenValue(form, "csrf-token");
        String prepared =
            post(
                http,
                "/s3/transfer",
                Map.of(
                    "csrf-token", csrf,
                    "bucket", sourceBucket,
                    "key", sourceKey,
                    "move", "false",
                    "destinationBucket", destinationBucket,
                    "destinationKey", destinationKey,
                    "decision", "keep",
                    "lang", "en",
                    "theme", "dark",
                    "density", "compact"));
        String operationId = attribute(prepared, "data-operation-id");
        assertEquals("READY", awaitOperationState(http, operationId, "READY"));

        String review =
            get(
                http,
                "/operations/" + operationId + "/review?lang=en&theme=dark&density=compact");
        assertTrue(review.contains(sourceBucket), review);
        assertTrue(review.contains(sourceKey), review);
        assertTrue(review.contains(destinationBucket), review);
        assertTrue(review.contains(destinationKey), review);
        post(
            http,
            "/operations/" + operationId + "/start",
            Map.of(
                "csrf-token", hiddenValue(review, "csrf-token"),
                "lang", "en",
                "theme", "dark",
                "density", "compact"));
        assertEquals("SUCCEEDED", awaitOperationState(http, operationId, "SUCCEEDED"));

        String completedReview =
            get(
                http,
                "/operations/" + operationId + "/review?lang=en&theme=dark&density=compact");
        assertTrue(completedReview.contains(sourceBucket), completedReview);
        assertTrue(completedReview.contains(sourceKey), completedReview);
        assertEquals(
            body,
            s3.getObjectAsBytes(
                    request -> request.bucket(destinationBucket).key(destinationKey))
                .asUtf8String());
      } finally {
        if (sourceCreated) cleanupBucket(s3, sourceBucket);
        if (destinationCreated) cleanupBucket(s3, destinationBucket);
      }
    }
  }

  static void verifyAwsCli(String suffix, Path temporaryDirectory) throws Exception {
    Path script = Path.of("..", "scripts", "aws-contract.sh").toAbsolutePath().normalize();
    assertTrue(Files.isRegularFile(script), "AWS CLI contract script is missing: " + script);

    Path output = Files.createTempFile(temporaryDirectory, "aws-contract-", ".log");
    try {
      ProcessBuilder builder =
          new ProcessBuilder("bash", script.toString()).redirectErrorStream(true).redirectOutput(output.toFile());
      builder.environment().put("STACKEMU_ENDPOINT", ENDPOINT.toString());
      builder.environment().put("STACKEMU_AWS_TEST_ID", suffix);
      builder.environment().put("AWS_ACCESS_KEY_ID", "test");
      builder.environment().put("AWS_SECRET_ACCESS_KEY", "test");
      builder.environment().put("AWS_DEFAULT_REGION", REGION.id());
      builder.environment().put("AWS_EC2_METADATA_DISABLED", "true");
      Process process = builder.start();
      if (!process.waitFor(120, TimeUnit.SECONDS)) {
        process.destroyForcibly();
        fail("AWS CLI contract script timed out: " + Files.readString(output, StandardCharsets.UTF_8));
      }
      assertEquals(0, process.exitValue(), Files.readString(output, StandardCharsets.UTF_8));
    } finally {
      Files.deleteIfExists(output);
    }
  }

  private static void verifyS3(S3Client s3, String suffix) {
    String bucket = "contract-" + suffix;
    String key = "contract-object.txt";
    try {
      s3.createBucket(request -> request.bucket(bucket));
      assertTrue(
          s3.listBuckets().buckets().stream().anyMatch(item -> bucket.equals(item.name())),
          "S3 bucket list omitted the created bucket");

      s3.putObject(
          PutObjectRequest.builder().bucket(bucket).key(key).build(),
          RequestBody.fromString("stackemu-contract-" + suffix));
      assertTrue(
          s3.listObjectsV2(ListObjectsV2Request.builder().bucket(bucket).build()).contents().stream()
              .anyMatch(item -> key.equals(item.key())),
          "S3 object list omitted the created object");

      s3.deleteObject(request -> request.bucket(bucket).key(key));
      assertTrue(
          s3.listObjectsV2(ListObjectsV2Request.builder().bucket(bucket).build()).contents().isEmpty(),
          "S3 object delete left the object visible");
      s3.deleteBucket(request -> request.bucket(bucket));
      assertTrue(
          s3.listBuckets().buckets().stream().noneMatch(item -> bucket.equals(item.name())),
          "S3 bucket delete left the bucket visible");
    } finally {
      ignoreFailure(() -> s3.deleteObject(request -> request.bucket(bucket).key(key)));
      ignoreFailure(() -> s3.deleteBucket(request -> request.bucket(bucket)));
    }
  }

  private static void verifySnsAndSqs(SnsClient sns, SqsClient sqs, String suffix) {
    String queueName = "contract-" + suffix;
    String topicName = "contract-" + suffix;
    String directMessage = "stackemu-sqs-contract-" + UUID.randomUUID();
    String notification = "stackemu-sns-contract-" + UUID.randomUUID();
    String queueUrl = null;
    String topicArn = null;
    String subscriptionArn = null;
    try {
      queueUrl = sqs.createQueue(request -> request.queueName(queueName)).queueUrl();
      String createdQueueUrl = queueUrl;
      assertTrue(
          sqs.listQueues(request -> request.queueNamePrefix(queueName)).queueUrls().stream()
              .anyMatch(createdQueueUrl::equals),
          "SQS queue list omitted the created queue");

      sqs.sendMessage(request -> request.queueUrl(createdQueueUrl).messageBody(directMessage));
      assertEquals(directMessage, receiveAndDelete(sqs, createdQueueUrl, directMessage));

      String queueArn =
          sqs.getQueueAttributes(
                  GetQueueAttributesRequest.builder()
                      .queueUrl(createdQueueUrl)
                      .attributeNames(QueueAttributeName.QUEUE_ARN)
                      .build())
              .attributesAsStrings()
              .get("QueueArn");
      topicArn = sns.createTopic(request -> request.name(topicName)).topicArn();
      String createdTopicArn = topicArn;
      assertTrue(
          sns.listTopics().topics().stream().anyMatch(item -> createdTopicArn.equals(item.topicArn())),
          "SNS topic list omitted the created topic");

      subscriptionArn =
          sns.subscribe(
                  SubscribeRequest.builder()
                      .topicArn(createdTopicArn)
                      .protocol("sqs")
                      .endpoint(queueArn)
                      .build())
              .subscriptionArn();
      sns.publish(request -> request.topicArn(createdTopicArn).message(notification));
      assertTrue(
          receiveAndDelete(sqs, createdQueueUrl, notification).contains(notification),
          "SNS message was not delivered to SQS");

      sns.unsubscribe(UnsubscribeRequest.builder().subscriptionArn(subscriptionArn).build());
      subscriptionArn = null;
      sns.deleteTopic(DeleteTopicRequest.builder().topicArn(createdTopicArn).build());
      topicArn = null;
      assertTrue(
          sns.listTopics().topics().stream().noneMatch(item -> createdTopicArn.equals(item.topicArn())),
          "SNS topic delete left the topic visible");

      sqs.deleteQueue(DeleteQueueRequest.builder().queueUrl(createdQueueUrl).build());
      queueUrl = null;
      assertTrue(
          sqs.listQueues(request -> request.queueNamePrefix(queueName)).queueUrls().stream()
              .noneMatch(createdQueueUrl::equals),
          "SQS queue delete left the queue visible");
    } finally {
      cleanupSubscription(sns, subscriptionArn);
      cleanupTopic(sns, topicArn);
      cleanupQueue(sqs, queueUrl);
    }
  }

  private static String receiveAndDelete(SqsClient sqs, String queueUrl, String expectedContent) {
    long deadline = System.nanoTime() + MESSAGE_TIMEOUT.toNanos();
    while (System.nanoTime() < deadline) {
      var messages =
          sqs.receiveMessage(
                  ReceiveMessageRequest.builder()
                      .queueUrl(queueUrl)
                      .maxNumberOfMessages(1)
                      .waitTimeSeconds(1)
                      .build())
              .messages();
      for (var message : messages) {
        if (message.body().contains(expectedContent)) {
          assertTrue(
              System.nanoTime() <= deadline,
              "SQS message arrived after the ten-second contract window");
          sqs.deleteMessage(
              DeleteMessageRequest.builder()
                  .queueUrl(queueUrl)
                  .receiptHandle(message.receiptHandle())
                  .build());
          return message.body();
        }
      }
    }
    return fail("SQS did not receive the expected message within ten seconds: " + expectedContent);
  }

  private static S3Client s3() {
    return S3Client.builder()
        .endpointOverride(ENDPOINT)
        .region(REGION)
        .credentialsProvider(credentials())
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
        .httpClientBuilder(UrlConnectionHttpClient.builder())
        .overrideConfiguration(timeouts())
        .build();
  }

  private static SqsClient sqs() {
    return SqsClient.builder()
        .endpointOverride(ENDPOINT)
        .region(REGION)
        .credentialsProvider(credentials())
        .httpClientBuilder(UrlConnectionHttpClient.builder())
        .overrideConfiguration(timeouts())
        .build();
  }

  private static SnsClient sns() {
    return SnsClient.builder()
        .endpointOverride(ENDPOINT)
        .region(REGION)
        .credentialsProvider(credentials())
        .httpClientBuilder(UrlConnectionHttpClient.builder())
        .overrideConfiguration(timeouts())
        .build();
  }

  private static StaticCredentialsProvider credentials() {
    return StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"));
  }

  private static ClientOverrideConfiguration timeouts() {
    return ClientOverrideConfiguration.builder()
        .apiCallTimeout(Duration.ofSeconds(10))
        .apiCallAttemptTimeout(Duration.ofSeconds(5))
        .build();
  }

  private static String get(HttpClient client, String path) throws Exception {
    HttpResponse<String> response =
        client.send(
            HttpRequest.newBuilder(CONSOLE.resolve(path))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    assertEquals(200, response.statusCode(), response.body());
    return response.body();
  }

  private static String post(HttpClient client, String path, Map<String, String> fields)
      throws Exception {
    HttpResponse<String> response =
        client.send(
            HttpRequest.newBuilder(CONSOLE.resolve(path))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(query(fields), StandardCharsets.UTF_8))
                .build(),
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    assertEquals(200, response.statusCode(), response.body());
    return response.body();
  }

  private static String query(Map<String, String> fields) {
    return fields.entrySet().stream()
        .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
        .collect(java.util.stream.Collectors.joining("&"));
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private static String hiddenValue(String html, String name) {
    Matcher matcher =
        Pattern.compile("name=\"" + Pattern.quote(name) + "\"\\s+value=\"([^\"]+)\"")
            .matcher(html);
    if (!matcher.find()) return fail("Could not find form field " + name + " in console response");
    return matcher.group(1);
  }

  private static String attribute(String html, String name) {
    Matcher matcher =
        Pattern.compile(Pattern.quote(name) + "=\"([a-f0-9-]{36})\"").matcher(html);
    if (!matcher.find()) return fail("Could not find attribute " + name + " in console response");
    return matcher.group(1);
  }

  private static String awaitOperationState(HttpClient http, String id, String expected)
      throws Exception {
    long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
    String state = "UNKNOWN";
    while (System.nanoTime() < deadline) {
      String json = get(http, "/operations/" + id + "/state");
      Matcher matcher = Pattern.compile("\"state\":\"([A-Z_]+)\"").matcher(json);
      if (!matcher.find()) return fail("Console operation state was not JSON: " + json);
      state = matcher.group(1);
      if (state.equals(expected) || state.equals("FAILED") || state.equals("PARTIAL")) return state;
      Thread.sleep(100);
    }
    return fail("Console operation did not reach " + expected + " (last state: " + state + ")");
  }

  private static void cleanupBucket(S3Client s3, String bucket) {
    ignoreFailure(
        () -> {
          for (var object : s3.listObjectsV2(request -> request.bucket(bucket)).contents()) {
            s3.deleteObject(request -> request.bucket(bucket).key(object.key()));
          }
          s3.deleteBucket(request -> request.bucket(bucket));
        });
  }

  private static void cleanupSubscription(SnsClient sns, String subscriptionArn) {
    if (subscriptionArn != null) {
      ignoreFailure(() -> sns.unsubscribe(request -> request.subscriptionArn(subscriptionArn)));
    }
  }

  private static void cleanupTopic(SnsClient sns, String topicArn) {
    if (topicArn != null) ignoreFailure(() -> sns.deleteTopic(request -> request.topicArn(topicArn)));
  }

  private static void cleanupQueue(SqsClient sqs, String queueUrl) {
    if (queueUrl != null) ignoreFailure(() -> sqs.deleteQueue(request -> request.queueUrl(queueUrl)));
  }

  private static void ignoreFailure(Runnable cleanup) {
    try {
      cleanup.run();
    } catch (RuntimeException ignored) {
      // Cleanup targets only resources created by this unique test fixture.
    }
  }
}
