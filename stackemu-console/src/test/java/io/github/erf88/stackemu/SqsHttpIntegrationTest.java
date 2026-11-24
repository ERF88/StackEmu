package io.github.erf88.stackemu;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

/** HTTP coverage for the local SQS pages. It never relies on a shared queue. */
@QuarkusTest
@EnabledIfEnvironmentVariable(named = "STACKEMU_INTEGRATION", matches = "true")
class SqsHttpIntegrationTest {
  @Inject SqsClient client;
  private String queue;
  private String fifoQueue;

  @BeforeEach
  void createFixture() {
    queue = "stackemu-sqs-http-" + UUID.randomUUID();
    client.createQueue(request -> request.queueName(queue));
  }

  @AfterEach
  void removeFixture() {
    removeQueue(queue);
    removeQueue(fifoQueue);
  }

  @Test
  void listAndDetailPreservePreferencesAndShowOnlySqsContext() {
    given()
        .queryParam("lang", "pt-BR")
        .queryParam("theme", "light")
        .get("/sqs")
        .then()
        .statusCode(200)
        .header("Cache-Control", "no-store")
        .body(
            containsString(queue),
            containsString("Criar fila"),
            containsString("lang=pt-BR"),
            not(containsString("Could not verify the local SQS emulator")));
    given()
        .queryParam("name", queue)
        .queryParam("lang", "pt-BR")
        .queryParam("theme", "light")
        .get("/sqs/queue")
        .then()
        .statusCode(200)
        .body(
            containsString(queue), containsString("Configurar"), containsString("Enviar mensagem"));
  }

  @Test
  void contextualDiagnosticOnlyUsesSqsAndDoesNotClaimSuccessWhenUnavailable() {
    given()
        .queryParam("lang", "en")
        .get("/sqs/connection")
        .then()
        .statusCode(200)
        .body(containsString("Connected to the local SQS emulator"));
  }

  @Test
  void standardQueueSendFormHidesFifoOnlyFields() {
    given()
        .queryParam("action", "send")
        .queryParam("queue", queue)
        .queryParam("lang", "pt-BR")
        .get("/sqs/actions")
        .then()
        .statusCode(200)
        .body(
            containsString("Enviar mensagem"),
            not(containsString("ID do grupo de mensagens (FIFO)")),
            not(containsString("ID de desduplicação")));
  }

  @Test
  void fifoQueueSendFormShowsFifoOnlyFields() {
    fifoQueue = "stackemu-sqs-http-" + UUID.randomUUID() + ".fifo";
    client.createQueue(
        request ->
            request.queueName(fifoQueue).attributes(Map.of(QueueAttributeName.FIFO_QUEUE, "true")));

    given()
        .queryParam("action", "send")
        .queryParam("queue", fifoQueue)
        .queryParam("lang", "pt-BR")
        .get("/sqs/actions")
        .then()
        .statusCode(200)
        .body(
            containsString("ID do grupo de mensagens (FIFO)"),
            containsString(
                "ID de desduplicação (obrigatório sem desduplicação baseada no conteúdo)"));
  }

  @Test
  void missingQueueUsesTheLocalizedStableReadError() {
    given()
        .queryParam("name", "stackemu-missing-queue")
        .queryParam("lang", "pt-BR")
        .get("/sqs/queue")
        .then()
        .statusCode(404)
        .header("Cache-Control", "no-store")
        .body(containsString("A fila não foi encontrada."), containsString("Voltar ao SQS"));
  }

  private void removeQueue(String name) {
    if (name == null) return;
    try {
      client.deleteQueue(
          request -> request.queueUrl(client.getQueueUrl(url -> url.queueName(name)).queueUrl()));
    } catch (RuntimeException ignored) {
      // The fixture is namespaced and isolated from user queues.
    }
  }
}
