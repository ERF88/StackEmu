package io.github.erf88.stackemu.adapter.web.sqs;

import io.github.erf88.stackemu.adapter.web.UiContext;
import io.github.erf88.stackemu.application.sqs.SqsException;
import io.github.erf88.stackemu.application.sqs.SqsMessagePort;
import io.github.erf88.stackemu.application.sqs.SqsQueueCreation;
import io.github.erf88.stackemu.application.sqs.SqsQueueCreationException;
import io.github.erf88.stackemu.application.sqs.SqsQueuePort;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Separate SQS mutation forms. They retain browser-native POST/CSRF handling. */
@Path("/sqs/actions")
@Blocking
@Produces(MediaType.TEXT_HTML)
public class SqsActionsResource {
  private final SqsQueuePort queues;
  private final SqsMessagePort messages;
  private final SqsQueueCreation queueCreation;

  public SqsActionsResource(SqsQueuePort queues, SqsMessagePort messages) {
    this.queues = queues;
    this.messages = messages;
    this.queueCreation = new SqsQueueCreation(queues);
  }

  @CheckedTemplate
  public static class Templates {
    public static native TemplateInstance form(
        String action, String queue, boolean fifo, String message, UiContext ui);

    public static native TemplateInstance received(
        String queue, List<SqsMessagePort.ReceivedMessage> messages, UiContext ui);

    public static native TemplateInstance configure(
        SqsQueuePort.Queue queue,
        List<SqsQueuePort.Queue> deadLetterQueues,
        String configuredTags,
        String configuredDeadLetterQueue,
        int configuredMaxReceiveCount,
        String message,
        UiContext ui);

    public static native TemplateInstance batchResult(
        String queue, SqsMessagePort.BatchResult result, UiContext ui);

    public static native TemplateInstance result(
        String title, String message, String returnHref, UiContext ui);

    public static native TemplateInstance partialResult(
        String queue,
        String deadLetterQueue,
        boolean queueCreated,
        String returnHref,
        UiContext ui);
  }

  @GET
  public Response form(
      @QueryParam("action") String action,
      @QueryParam("queue") String queue,
      @QueryParam("lang") String language,
      @QueryParam("theme") String theme,
      @QueryParam("density") String density) {
    if ("configure".equals(action)) {
      SqsQueuePort.Queue selected = queues.getQueue(queue);
      Optional<SqsQueuePort.RedrivePolicy> redrivePolicy = selected.redrivePolicy();
      return SqsWebSupport.workspace(
          Templates.configure(
              selected,
              queues.listQueues().stream()
                  .filter(candidate -> !candidate.name().equals(selected.name()))
                  .filter(candidate -> candidate.fifo() == selected.fifo())
                  .toList(),
              String.join(
                  "\n",
                  selected.tags().entrySet().stream()
                      .map(tag -> tag.getKey() + "=" + tag.getValue())
                      .toList()),
              redrivePolicy.map(SqsQueuePort.RedrivePolicy::deadLetterQueueName).orElse(""),
              redrivePolicy.map(SqsQueuePort.RedrivePolicy::maxReceiveCount).orElse(5),
              "",
              UiContext.resolve(language, null, theme, density, "/sqs/actions")));
    }
    return SqsWebSupport.workspace(
        Templates.form(
            normalize(action),
            safe(queue),
            fifoQueue(queue),
            "",
            UiContext.resolve(language, null, theme, density, "/sqs/actions")));
  }

  @POST
  @Path("create")
  public Response create(
      @FormParam("name") String name,
      @FormParam("fifo") boolean fifo,
      @FormParam("createDlq") boolean createDlq,
      @FormParam("maxReceiveCount") Integer maxReceiveCount,
      @HeaderParam("HX-Request") String htmxRequest,
      @FormParam("lang") String language,
      @FormParam("theme") String theme,
      @FormParam("density") String density) {
    String queueName = fifo && name != null && !name.endsWith(".fifo") ? name + ".fifo" : name;
    UiContext ui = UiContext.resolve(language, null, theme, density, "/sqs/actions");
    try {
      if (createDlq) {
        int receiveLimit = maxReceiveCount == null ? 5 : maxReceiveCount;
        queueCreation.createWithDeadLetterQueue(queueName, receiveLimit);
      } else {
        queueCreation.create(queueName);
      }
      return completed(
          htmxRequest,
          ui.text("sqsCreateQueue"),
          queueName,
          SqsWebSupport.serviceHref(language, theme, density),
          SqsWebSupport.queueRedirect(queueName, language, theme, density),
          ui);
    } catch (SqsQueueCreationException partialFailure) {
      return partialCreation(partialFailure, language, theme, density, ui);
    } catch (SqsException failure) {
      return actionError(failure, "create", "", ui);
    }
  }

  @POST
  @Path("send")
  public Response send(
      @FormParam("queue") String queue,
      @FormParam("body") String body,
      @FormParam("groupId") String groupId,
      @FormParam("deduplicationId") String deduplicationId,
      @HeaderParam("HX-Request") String htmxRequest,
      @FormParam("lang") String language,
      @FormParam("theme") String theme,
      @FormParam("density") String density) {
    UiContext ui = UiContext.resolve(language, null, theme, density, "/sqs/actions");
    try {
      messages.send(
          queue,
          new SqsMessagePort.OutboundMessage(
              body, Map.of(), optional(groupId), optional(deduplicationId)));
      return completed(
          htmxRequest,
          ui.text("sqsSendMessage"),
          queue,
          SqsWebSupport.queueHref(queue, language, theme, density),
          SqsWebSupport.queueRedirect(queue, language, theme, density),
          ui);
    } catch (SqsException failure) {
      return actionError(failure, "send", safe(queue), ui);
    }
  }

  @POST
  @Path("send-batch")
  public Response sendBatch(
      @FormParam("queue") String queue,
      @FormParam("bodies") String bodies,
      @FormParam("groupId") String groupId,
      @FormParam("lang") String language,
      @FormParam("theme") String theme,
      @FormParam("density") String density) {
    List<SqsMessagePort.OutboundMessage> messages =
        bodies == null
            ? List.of()
            : bodies
                .lines()
                .filter(line -> !line.isEmpty())
                .map(
                    body ->
                        new SqsMessagePort.OutboundMessage(
                            body, Map.of(), optional(groupId), Optional.empty()))
                .toList();
    UiContext ui = UiContext.resolve(language, null, theme, density, "/sqs/actions");
    try {
      return SqsWebSupport.workspace(
          Templates.batchResult(queue, this.messages.sendBatch(queue, messages), ui));
    } catch (SqsException failure) {
      return actionError(failure, "send-batch", safe(queue), ui);
    }
  }

  @POST
  @Path("receive")
  public Response receive(
      @FormParam("queue") String queue,
      @FormParam("maximumMessages") int maximumMessages,
      @FormParam("visibilityTimeout") int visibilityTimeout,
      @FormParam("lang") String language,
      @FormParam("theme") String theme,
      @FormParam("density") String density) {
    UiContext ui = UiContext.resolve(language, null, theme, density, "/sqs/actions");
    try {
      return SqsWebSupport.workspace(
          Templates.received(
              queue, messages.receive(queue, maximumMessages, visibilityTimeout), ui));
    } catch (SqsException failure) {
      return actionError(failure, "receive", safe(queue), ui);
    }
  }

  @POST
  @Path("delete-message")
  public Response deleteMessage(
      @FormParam("queue") String queue,
      @FormParam("receiptHandle") String receiptHandle,
      @HeaderParam("HX-Request") String htmxRequest,
      @FormParam("lang") String language,
      @FormParam("theme") String theme,
      @FormParam("density") String density) {
    UiContext ui = UiContext.resolve(language, null, theme, density, "/sqs/actions");
    try {
      messages.delete(queue, receiptHandle);
      return completed(
          htmxRequest,
          ui.text("sqsDeleteSelected"),
          queue,
          SqsWebSupport.queueHref(queue, language, theme, density),
          SqsWebSupport.queueRedirect(queue, language, theme, density),
          ui);
    } catch (SqsException failure) {
      return actionError(failure, "receive", safe(queue), ui);
    }
  }

  @POST
  @Path("delete-messages")
  public Response deleteMessages(
      @FormParam("queue") String queue,
      @FormParam("receiptHandle") List<String> receiptHandles,
      @FormParam("lang") String language,
      @FormParam("theme") String theme,
      @FormParam("density") String density) {
    UiContext ui = UiContext.resolve(language, null, theme, density, "/sqs/actions");
    try {
      return SqsWebSupport.workspace(
          Templates.batchResult(
              queue,
              messages.deleteBatch(queue, receiptHandles == null ? List.of() : receiptHandles),
              ui));
    } catch (SqsException failure) {
      return actionError(failure, "receive", safe(queue), ui);
    }
  }

  @POST
  @Path("configure")
  public Response configure(
      @FormParam("queue") String queue,
      @FormParam("visibilityTimeout") int visibilityTimeout,
      @FormParam("retentionPeriod") int retentionPeriod,
      @FormParam("delaySeconds") int delaySeconds,
      @FormParam("maximumMessageSize") int maximumMessageSize,
      @FormParam("receiveWaitTime") int receiveWaitTime,
      @FormParam("contentBasedDeduplication") boolean contentBasedDeduplication,
      @FormParam("managedSse") boolean managedSse,
      @FormParam("tags") String tags,
      @FormParam("deadLetterQueue") String deadLetterQueue,
      @FormParam("maxReceiveCount") Integer maxReceiveCount,
      @HeaderParam("HX-Request") String htmxRequest,
      @FormParam("lang") String language,
      @FormParam("theme") String theme,
      @FormParam("density") String density) {
    UiContext ui = UiContext.resolve(language, null, theme, density, "/sqs/actions");
    try {
      SqsQueuePort.Queue existing = queues.getQueue(queue);
      var settings =
          new SqsQueuePort.Settings(
              visibilityTimeout,
              retentionPeriod,
              delaySeconds,
              maximumMessageSize,
              receiveWaitTime,
              contentBasedDeduplication,
              managedSse);
      Optional<SqsQueuePort.RedrivePolicy> redrive =
          optional(deadLetterQueue)
              .map(
                  target ->
                      new SqsQueuePort.RedrivePolicy(
                          target, maxReceiveCount == null ? 5 : maxReceiveCount));
      queues.updateQueue(queue, settings, tags(tags), redrive);
      return completed(
          htmxRequest,
          ui.text("sqsSaveSettings"),
          existing.name(),
          SqsWebSupport.queueHref(existing.name(), language, theme, density),
          SqsWebSupport.queueRedirect(existing.name(), language, theme, density),
          ui);
    } catch (SqsException failure) {
      return actionError(failure, "configure", safe(queue), ui);
    }
  }

  @POST
  @Path("purge")
  public Response purge(
      @FormParam("queue") String queue,
      @FormParam("confirmName") String confirmation,
      @HeaderParam("HX-Request") String htmxRequest,
      @FormParam("lang") String language,
      @FormParam("theme") String theme,
      @FormParam("density") String density) {
    UiContext ui = UiContext.resolve(language, null, theme, density, "/sqs/actions");
    try {
      exact(queue, confirmation);
      queues.purgeQueue(queue);
      return completed(
          htmxRequest,
          ui.text("sqsPurgeQueue"),
          queue,
          SqsWebSupport.queueHref(queue, language, theme, density),
          SqsWebSupport.queueRedirect(queue, language, theme, density),
          ui);
    } catch (SqsException failure) {
      return actionError(failure, "purge", safe(queue), ui);
    }
  }

  @POST
  @Path("delete-queue")
  public Response deleteQueue(
      @FormParam("queue") String queue,
      @FormParam("confirmName") String confirmation,
      @HeaderParam("HX-Request") String htmxRequest,
      @FormParam("lang") String language,
      @FormParam("theme") String theme,
      @FormParam("density") String density) {
    UiContext ui = UiContext.resolve(language, null, theme, density, "/sqs/actions");
    try {
      exact(queue, confirmation);
      queues.deleteQueue(queue);
      return completed(
          htmxRequest,
          ui.text("sqsDeleteQueue"),
          queue,
          SqsWebSupport.serviceHref(language, theme, density),
          SqsWebSupport.serviceRedirect(language, theme, density),
          ui);
    } catch (SqsException failure) {
      return actionError(failure, "delete-queue", safe(queue), ui);
    }
  }

  private static Optional<String> optional(String value) {
    return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
  }

  private static String normalize(String action) {
    return List.of("create", "send", "send-batch", "receive", "configure", "purge", "delete-queue")
            .contains(action)
        ? action
        : "create";
  }

  private static String safe(String value) {
    return value == null ? "" : value;
  }

  private static boolean fifoQueue(String queue) {
    return queue != null && queue.endsWith(".fifo");
  }

  private static Map<String, String> tags(String source) {
    if (source == null || source.isBlank()) return Map.of();
    Map<String, String> tags = new java.util.LinkedHashMap<>();
    for (String line : source.split("\\R")) {
      int separator = line.indexOf('=');
      if (separator <= 0 || separator == line.length() - 1 || tags.size() == 10)
        throw new SqsException(SqsException.Kind.INVALID);
      String key = line.substring(0, separator).trim();
      String value = line.substring(separator + 1).trim();
      if (!key.matches("[A-Za-z0-9_.:/=+@-]{1,128}") || value.length() > 256)
        throw new SqsException(SqsException.Kind.INVALID);
      tags.put(key, value);
    }
    return Map.copyOf(tags);
  }

  private static void exact(String queue, String confirmation) {
    if (queue == null || !queue.equals(confirmation))
      throw new SqsException(SqsException.Kind.INVALID);
  }

  private static Response completed(
      String htmxRequest,
      String title,
      String message,
      String returnHref,
      Response nativeResponse,
      UiContext ui) {
    return "true".equals(htmxRequest)
        ? SqsWebSupport.workspace(Templates.result(title, message, returnHref, ui))
        : nativeResponse;
  }

  private static Response partialCreation(
      SqsQueueCreationException failure,
      String language,
      String theme,
      String density,
      UiContext ui) {
    TemplateInstance result =
        Templates.partialResult(
            failure.queueName(),
            failure.deadLetterQueueName(),
            failure.queueCreated(),
            SqsWebSupport.serviceHref(language, theme, density),
            ui);
    return SqsWebSupport.workspace(SqsWebSupport.status(failure.failure()), result);
  }

  private Response actionError(SqsException failure, String action, String queue, UiContext ui) {
    if ("configure".equals(action)) {
      try {
        SqsQueuePort.Queue selected = queues.getQueue(queue);
        Optional<SqsQueuePort.RedrivePolicy> redrivePolicy = selected.redrivePolicy();
        return SqsWebSupport.workspace(
            SqsWebSupport.status(failure),
            Templates.configure(
                selected,
                queues.listQueues().stream()
                    .filter(candidate -> !candidate.name().equals(selected.name()))
                    .filter(candidate -> candidate.fifo() == selected.fifo())
                    .toList(),
                String.join(
                    "\n",
                    selected.tags().entrySet().stream()
                        .map(tag -> tag.getKey() + "=" + tag.getValue())
                        .toList()),
                redrivePolicy.map(SqsQueuePort.RedrivePolicy::deadLetterQueueName).orElse(""),
                redrivePolicy.map(SqsQueuePort.RedrivePolicy::maxReceiveCount).orElse(5),
                ui.text(SqsWebSupport.messageKey(failure)),
                ui));
      } catch (SqsException ignored) {
        // The regular workspace below reports an unavailable configuration target.
      }
    }
    return SqsWebSupport.workspace(
        SqsWebSupport.status(failure),
        Templates.form(
            action, queue, fifoQueue(queue), ui.text(SqsWebSupport.messageKey(failure)), ui));
  }
}
