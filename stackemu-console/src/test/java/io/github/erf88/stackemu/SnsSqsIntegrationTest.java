package io.github.erf88.stackemu;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.erf88.stackemu.adapter.web.UiContext;
import io.github.erf88.stackemu.adapter.web.sns.SnsResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
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
  private String topicName;
  private String extraQueueUrl;

  @Test
  void snsDescriptionsAndEmptyStateUseConsistentCopyInBothLanguages() {
    for (String language : List.of("en", "pt-BR")) {
      UiContext ui = UiContext.resolve(language, null, "dark", "/sns");
      String emptyList = SnsResource.Templates.index(List.of(), "", ui).render();

      assertTrue(emptyList.contains(ui.text("snsDescription")));
      assertTrue(emptyList.contains(ui.text("snsNoTopicsHelp")));
      for (String key :
          List.of(
              "snsDescription",
              "snsNoTopicsHelp",
              "snsTopicDescription",
              "snsPublishHelp",
              "snsQueueListUnavailable",
              "snsUnavailable")) {
        assertTrue(
            !ui.text(key).toLowerCase(Locale.ROOT).contains("local"),
            () -> "Unexpected local wording in " + key + " for " + language);
      }
    }
  }

  @AfterEach
  void removeOwnResources() {
    try (SnsClient sns = sns()) {
      if (subscriptionArn != null) {
        try {
          sns.unsubscribe(UnsubscribeRequest.builder().subscriptionArn(subscriptionArn).build());
        } catch (RuntimeException ignored) {
          // Cleanup never targets resources outside this namespaced fixture.
        }
      }
      if (topicArn != null) {
        try {
          sns.deleteTopic(DeleteTopicRequest.builder().topicArn(topicArn).build());
        } catch (RuntimeException ignored) {
          // Cleanup never targets resources outside this namespaced fixture.
        }
      }
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
    if (extraQueueUrl != null) {
      try {
        sqs.deleteQueue(DeleteQueueRequest.builder().queueUrl(extraQueueUrl).build());
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

  @Test
  void snsActionsUseSharedNoticesAndDeleteShowsCopyableTopicName() {
    String suffix = UUID.randomUUID().toString();
    topicName = "stackemu-sns-web-" + suffix;
    var createForm = form("/sns/actions?action=create");
    var created =
        submit(
            "/sns/actions/create",
            createForm,
            Map.of("name", topicName, "lang", "en", "theme", "dark"));
    try (SnsClient client = sns()) {
      topicArn =
          client.listTopics().topics().stream()
              .filter(candidate -> candidate.topicArn().endsWith(":" + topicName))
              .map(topic -> topic.topicArn())
              .findFirst()
              .orElseThrow();
    }
    assertNotice(created, "Topic created.");

    String snsListPage =
        given()
            .queryParam("lang", "en")
            .queryParam("theme", "dark")
            .get("/sns")
            .then()
            .statusCode(200)
            .extract()
            .asString();
    assertTrue(snsListPage.contains("aria-label=\"Publish message: " + topicName + "\""));
    assertTrue(snsListPage.contains("aria-label=\"Subscribe queue: " + topicName + "\""));
    assertTrue(snsListPage.contains("aria-label=\"Delete topic: " + topicName + "\""));
    assertTrue(snsListPage.contains("Publish messages and connect topics to SQS queues."));

    String topicPage =
        given()
            .queryParam("name", topicName)
            .queryParam("lang", "en")
            .queryParam("theme", "dark")
            .get("/sns/topic")
            .then()
            .statusCode(200)
            .extract()
            .asString();
    assertTrue(topicPage.contains("Review deletion"));
    assertTrue(topicPage.contains("returnTo=topic"));
    assertTrue(topicPage.contains("Publish a message or connect a compatible SQS queue."));
    assertTrue(topicPage.contains("Send a test message to this topic."));

    String portugueseTopicPage =
        given()
            .queryParam("name", topicName)
            .queryParam("lang", "pt-BR")
            .queryParam("theme", "dark")
            .get("/sns/topic")
            .then()
            .statusCode(200)
            .extract()
            .asString();
    assertTrue(
        portugueseTopicPage.contains("Publique uma mensagem ou conecte uma fila SQS compatível."));
    assertTrue(portugueseTopicPage.contains("Envie uma mensagem de teste para este tópico."));

    String deletionReview =
        given()
            .queryParam("action", "delete")
            .queryParam("topic", topicName)
            .queryParam("returnTo", "list")
            .queryParam("lang", "en")
            .queryParam("theme", "dark")
            .get("/sns/actions")
            .then()
            .statusCode(200)
            .extract()
            .asString();
    assertTrue(deletionReview.contains("id=\"sns-topic-name-copy\""));
    assertTrue(deletionReview.contains("aria-label=\"Copy topic name\""));
    assertTrue(deletionReview.contains("data-copy-field"));
    assertTrue(deletionReview.contains(">" + topicName + "</textarea>"));
    assertTrue(deletionReview.contains("name=\"returnTo\" value=\"list\""));
    assertTrue(deletionReview.contains("href=\"/sns?lang=en&amp;theme=dark"));
    assertTrue(!deletionReview.contains("href=\"/sns/topic?name="));
    assertEquals(1, deletionReview.split("Topic name", -1).length - 1);

    String detailDeletionReview =
        given()
            .queryParam("action", "delete")
            .queryParam("topic", topicName)
            .queryParam("returnTo", "topic")
            .queryParam("lang", "en")
            .queryParam("theme", "dark")
            .get("/sns/actions")
            .then()
            .statusCode(200)
            .extract()
            .asString();
    assertTrue(detailDeletionReview.contains("href=\"/sns/topic?name=" + topicName));

    String mismatch = "wrong-" + topicName;
    var rejectedDelete =
        submit(
            "/sns/actions/delete",
            form(deleteActionPath()),
            Map.of(
                "topic", topicName,
                "confirmation", mismatch,
                "returnTo", "list",
                "lang", "en",
                "theme", "dark"));
    assertEquals(400, rejectedDelete.statusCode());
    assertTrue(rejectedDelete.asString().contains("role=\"alert\""));
    assertTrue(rejectedDelete.asString().contains("value=\"" + mismatch + "\""));
    assertTrue(rejectedDelete.asString().contains("name=\"returnTo\" value=\"list\""));
    assertTrue(rejectedDelete.asString().contains("href=\"/sns?lang=en&amp;theme=dark"));

    queueUrl =
        sqs.createQueue(request -> request.queueName("stackemu-sns-web-" + suffix)).queueUrl();
    var subscribe =
        submit(
            "/sns/actions/subscribe",
            form("/sns/actions?action=subscribe&topic=" + topicName),
            Map.of(
                "topic",
                topicName,
                "queue",
                "stackemu-sns-web-" + suffix,
                "lang",
                "en",
                "theme",
                "dark"));
    assertNotice(subscribe, "Queue subscribed.");

    var publish =
        submit(
            "/sns/actions/publish",
            form("/sns/actions?action=publish&topic=" + topicName),
            Map.of(
                "topic",
                topicName,
                "message",
                "StackEmu web notice",
                "lang",
                "en",
                "theme",
                "dark"));
    assertNotice(publish, "Message published.");

    try (SnsClient client = sns()) {
      subscriptionArn =
          client
              .listSubscriptionsByTopic(request -> request.topicArn(topicArn))
              .subscriptions()
              .stream()
              .filter(subscription -> subscription.protocol().equals("sqs"))
              .map(subscription -> subscription.subscriptionArn())
              .findFirst()
              .orElseThrow();
    }
    var unsubscribe =
        submit(
            "/sns/actions/unsubscribe",
            form(topicPath()),
            Map.of(
                "topic", topicName,
                "subscriptionArn", subscriptionArn,
                "confirm", "yes",
                "lang", "en",
                "theme", "dark"));
    assertNotice(unsubscribe, "Subscription removed.");
    subscriptionArn = null;

    var deleted =
        submit(
            "/sns/actions/delete",
            form(deleteActionPath()),
            Map.of(
                "topic", topicName,
                "confirmation", topicName,
                "returnTo", "list",
                "lang", "en",
                "theme", "dark"));
    assertNotice(deleted, "Topic deleted.");
    topicArn = null;
  }

  @Test
  void snsPublishAndSubscribeUseActionWorkspacesAndRefreshSubscriptions() {
    String suffix = UUID.randomUUID().toString();
    topicName = "stackemu-sns-modal-" + suffix;
    var createForm = form("/sns/actions?action=create");
    var created =
        submit(
            "/sns/actions/create",
            createForm,
            Map.of("name", topicName, "lang", "en", "theme", "dark"));
    assertEquals(303, created.statusCode());
    try (SnsClient client = sns()) {
      topicArn =
          client.listTopics().topics().stream()
              .filter(candidate -> candidate.topicArn().endsWith(":" + topicName))
              .map(topic -> topic.topicArn())
              .findFirst()
              .orElseThrow();
    }

    var publishForm = form("/sns/actions?action=publish&topic=" + topicName + "&returnTo=list");
    var publishPage =
        given()
            .header("HX-Request", "true")
            .queryParam("action", "publish")
            .queryParam("topic", topicName)
            .queryParam("returnTo", "list")
            .queryParam("lang", "en")
            .queryParam("theme", "dark")
            .get("/sns/actions")
            .then()
            .statusCode(200)
            .header("X-StackEmu-Action", "workspace")
            .extract()
            .asString();
    assertTrue(publishPage.contains("id=\"action-workspace\""));
    assertTrue(publishPage.contains("name=\"message\""));
    assertTrue(publishPage.contains("name=\"returnTo\" value=\"list\""));
    assertTrue(publishPage.contains("href=\"/sns?lang=en&amp;theme=dark"));
    assertTrue(!publishPage.contains("name=\"messageGroupId\""));
    var published =
        submitHtmx(
            "/sns/actions/publish",
            publishForm,
            Map.of(
                "topic",
                topicName,
                "message",
                "published from the action dialog",
                "returnTo",
                "list",
                "lang",
                "en",
                "theme",
                "dark"));
    assertEquals(200, published.statusCode());
    assertEquals("workspace", published.header("X-StackEmu-Action"));
    assertTrue(published.asString().contains("data-action-completed"));
    assertTrue(published.asString().contains("data-refresh-target=\"none\""));
    assertTrue(published.asString().contains("data-result-return=\"/sns?lang=en"));
    assertTrue(published.asString().contains("All topics</a>"));
    assertTrue(published.asString().contains("Message published."));

    var publishedWithRedirect =
        submit(
            "/sns/actions/publish",
            publishForm,
            Map.of(
                "topic", topicName,
                "message", "published from the list with a native form",
                "returnTo", "list",
                "lang", "en",
                "theme", "dark"));
    assertEquals(303, publishedWithRedirect.statusCode());
    assertTrue(publishedWithRedirect.header("Location").contains("/sns?lang=en"));
    assertTrue(publishedWithRedirect.header("Location").contains("notice=messagePublished"));

    queueUrl =
        sqs.createQueue(request -> request.queueName("stackemu-sns-modal-" + suffix)).queueUrl();
    var subscribeForm = form("/sns/actions?action=subscribe&topic=" + topicName);
    var subscribed =
        submitHtmx(
            "/sns/actions/subscribe",
            subscribeForm,
            Map.of(
                "topic",
                topicName,
                "queue",
                "stackemu-sns-modal-" + suffix,
                "lang",
                "en",
                "theme",
                "dark"));
    assertEquals(200, subscribed.statusCode());
    assertEquals("workspace", subscribed.header("X-StackEmu-Action"));
    assertTrue(subscribed.asString().contains("data-action-completed"));
    assertTrue(subscribed.asString().contains("data-refresh-target=\"sns-subscriptions\""));
    assertTrue(subscribed.asString().contains("data-result-return=\"/sns/topic?name="));

    var fragment =
        given()
            .queryParam("name", topicName)
            .queryParam("lang", "en")
            .queryParam("theme", "dark")
            .get("/sns/topic/subscriptions")
            .then()
            .statusCode(200)
            .header("X-StackEmu-Fragment", "sns-subscriptions")
            .extract()
            .asString();
    assertTrue(fragment.contains("id=\"sns-subscriptions-panel\""));
    assertTrue(fragment.contains("stackemu-sns-modal-" + suffix));
  }

  @Test
  void fifoActionWorkspacesShowFifoFieldsAndOnlyCompatibleQueues() {
    String suffix = UUID.randomUUID().toString();
    topicName = "stackemu-sns-action-" + suffix + ".fifo";
    try (SnsClient client = sns()) {
      topicArn =
          client
              .createTopic(
                  request ->
                      request
                          .name(topicName)
                          .attributes(
                              Map.of("FifoTopic", "true", "ContentBasedDeduplication", "false")))
              .topicArn();
    }

    String standardQueue = "stackemu-sns-action-standard-" + suffix;
    queueUrl = sqs.createQueue(request -> request.queueName(standardQueue)).queueUrl();
    String fifoQueue = "stackemu-sns-action-fifo-" + suffix + ".fifo";
    extraQueueUrl =
        sqs.createQueue(
                CreateQueueRequest.builder()
                    .queueName(fifoQueue)
                    .attributes(Map.of(QueueAttributeName.FIFO_QUEUE, "true"))
                    .build())
            .queueUrl();

    String publishPage =
        given()
            .queryParam("action", "publish")
            .queryParam("topic", topicName)
            .queryParam("lang", "en")
            .queryParam("theme", "dark")
            .get("/sns/actions")
            .then()
            .statusCode(200)
            .header("X-StackEmu-Action", "workspace")
            .extract()
            .asString();
    assertTrue(publishPage.contains("name=\"messageGroupId\""));
    assertTrue(publishPage.contains("name=\"messageDeduplicationId\""));
    var fifoPublishForm = form("/sns/actions?action=publish&topic=" + topicName);
    var invalidPublish =
        submitHtmx(
            "/sns/actions/publish",
            fifoPublishForm,
            Map.of(
                "topic",
                topicName,
                "message",
                "draft to preserve",
                "messageGroupId",
                "",
                "messageDeduplicationId",
                "deduplication-draft",
                "lang",
                "en",
                "theme",
                "dark"));
    assertEquals(400, invalidPublish.statusCode());
    assertEquals("workspace", invalidPublish.header("X-StackEmu-Action"));
    assertTrue(invalidPublish.asString().contains("draft to preserve</textarea>"));
    assertTrue(invalidPublish.asString().contains("value=\"deduplication-draft\""));

    String subscribePage =
        given()
            .queryParam("action", "subscribe")
            .queryParam("topic", topicName)
            .queryParam("lang", "en")
            .queryParam("theme", "dark")
            .get("/sns/actions")
            .then()
            .statusCode(200)
            .header("X-StackEmu-Action", "workspace")
            .extract()
            .asString();
    assertTrue(subscribePage.contains("value=\"" + fifoQueue + "\""));
    assertTrue(!subscribePage.contains("value=\"" + standardQueue + "\""));
  }

  private OperationHttpFixtures.Form form(String path) {
    var response =
        given()
            .queryParam("lang", "en")
            .queryParam("theme", "dark")
            .get(path)
            .then()
            .statusCode(200)
            .extract()
            .response();
    return new OperationHttpFixtures.Form(
        "", OperationHttpFixtures.field(response.asString(), "csrf-token"), response.cookies());
  }

  private Response submit(
      String route, OperationHttpFixtures.Form form, Map<String, String> fields) {
    var request =
        given()
            .cookies(form.cookies())
            .formParam("csrf-token", form.csrf())
            .redirects()
            .follow(false);
    fields.forEach(request::formParam);
    return request.post(route).then().extract().response();
  }

  private Response submitHtmx(
      String route, OperationHttpFixtures.Form form, Map<String, String> fields) {
    var request =
        given()
            .header("HX-Request", "true")
            .cookies(form.cookies())
            .formParam("csrf-token", form.csrf())
            .redirects()
            .follow(false);
    fields.forEach(request::formParam);
    return request.post(route).then().extract().response();
  }

  private void assertNotice(Response redirect, String message) {
    assertEquals(303, redirect.statusCode());
    assertTrue(redirect.header("Location").contains("notice="));
    var page = given().get(redirect.header("Location")).then().statusCode(200).extract().asString();
    assertTrue(page.contains("data-operation-success-message"));
    assertTrue(page.contains(message));
    assertTrue(page.contains("data-notice-dismiss"));
  }

  private String topicPath() {
    return "/sns/topic?name=" + topicName + "&lang=en&theme=dark";
  }

  private String deleteActionPath() {
    return "/sns/actions?action=delete&topic=" + topicName + "&returnTo=list&lang=en&theme=dark";
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
