package io.github.erf88.stackemu.adapter.web.sns;

import io.github.erf88.stackemu.adapter.web.UiContext;
import io.github.erf88.stackemu.application.sns.SnsException;
import io.github.erf88.stackemu.application.sns.SnsTopicPort;
import io.github.erf88.stackemu.application.sqs.SqsException;
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
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** The SNS console stays inside the local S3/SQS/SNS compatibility matrix. */
@Path("/sns")
@Blocking
@Produces(MediaType.TEXT_HTML)
public class SnsResource {
  private final SnsTopicPort topics;
  private final SqsQueuePort queues;

  public SnsResource(SnsTopicPort topics, SqsQueuePort queues) {
    this.topics = topics;
    this.queues = queues;
  }

  @CheckedTemplate
  public static class Templates {
    public static native TemplateInstance index(
        List<SnsTopicPort.Topic> topics, String notice, UiContext ui);

    public static native TemplateInstance create(
        UiContext ui, String actionError, String name, boolean fifo);

    public static native TemplateInstance delete(
        SnsTopicPort.Topic topic,
        String actionError,
        String confirmationDraft,
        String returnTo,
        String returnHref,
        String returnLabel,
        UiContext ui);

    public static native TemplateInstance topic(
        SnsTopicPort.Topic topic,
        List<SnsTopicPort.Subscription> subscriptions,
        List<SqsQueuePort.Queue> queues,
        String queueMessage,
        String notice,
        String actionError,
        String messageDraft,
        String selectedQueue,
        String confirmationDraft,
        String failedSubscriptionArn,
        boolean unsubscribeConfirmed,
        String messageGroupIdDraft,
        String messageDeduplicationIdDraft,
        UiContext ui);

    public static native TemplateInstance action(
        SnsTopicPort.Topic topic,
        List<SqsQueuePort.Queue> queues,
        String queueMessage,
        String action,
        String actionError,
        String messageDraft,
        String selectedQueue,
        String messageGroupIdDraft,
        String messageDeduplicationIdDraft,
        String returnTo,
        String returnHref,
        String returnLabel,
        UiContext ui);

    public static native TemplateInstance actionResult(
        String title,
        String message,
        String returnHref,
        String returnLabel,
        String refreshTarget,
        UiContext ui);

    public static native TemplateInstance actionUnavailable(
        String message, String returnHref, UiContext ui);

    public static native TemplateInstance subscriptions(
        SnsTopicPort.Topic topic,
        List<SnsTopicPort.Subscription> subscriptions,
        String confirmationDraft,
        String failedSubscriptionArn,
        boolean unsubscribeConfirmed,
        UiContext ui);

    public static native TemplateInstance error(String message, String returnHref, UiContext ui);
  }

  @GET
  @Path("actions")
  public Response actionForm(
      @QueryParam("action") String action,
      @QueryParam("topic") String topicName,
      @QueryParam("returnTo") String returnTo,
      @QueryParam("lang") String language,
      @QueryParam("theme") String theme,
      @QueryParam("density") String density,
      @HeaderParam("Accept-Language") String acceptLanguage) {
    UiContext ui = UiContext.resolve(language, acceptLanguage, theme, density, "/sns/actions");
    if ("publish".equals(action) || "subscribe".equals(action)) {
      return actionWorkspace(
          action,
          topicName,
          actionOrigin(returnTo),
          "",
          "",
          "",
          "",
          "",
          ui,
          Response.Status.OK.getStatusCode());
    }
    if ("delete".equals(action)) {
      String origin = actionOrigin(returnTo);
      try {
        return page(deleteReview(topicName, "", "", origin, ui));
      } catch (SnsException failure) {
        return error(
            failure, ui, actionReturnHref(origin, topicName, language, theme, density, null));
      }
    }
    if (!"create".equals(action)) {
      return page(
          Response.Status.NOT_FOUND.getStatusCode(),
          Templates.error(ui.text("snsInvalidRequest"), serviceHref(language, theme, density), ui));
    }
    return page(Templates.create(ui, "", "", false));
  }

  @GET
  public Response list(
      @QueryParam("lang") String language,
      @QueryParam("theme") String theme,
      @QueryParam("density") String density,
      @QueryParam("notice") String notice,
      @HeaderParam("Accept-Language") String acceptLanguage) {
    UiContext ui = UiContext.resolve(language, acceptLanguage, theme, density, "/sns");
    try {
      return page(Templates.index(topics.listTopics(), notice(notice, ui), ui));
    } catch (SnsException failure) {
      return error(failure, ui, serviceHref(language, theme, density));
    }
  }

  @GET
  @Path("topic")
  public Response topic(
      @QueryParam("name") String name,
      @QueryParam("lang") String language,
      @QueryParam("theme") String theme,
      @QueryParam("density") String density,
      @QueryParam("notice") String notice,
      @HeaderParam("Accept-Language") String acceptLanguage) {
    UiContext ui =
        UiContext.resolve(
            language, acceptLanguage, theme, density, "/sns/topic?name=" + encode(name));
    try {
      return page(topicTemplate(name, notice(notice, ui), ui));
    } catch (SnsException failure) {
      return error(failure, ui, serviceHref(language, theme, density));
    }
  }

  @GET
  @Path("topic/subscriptions")
  public Response subscriptions(
      @QueryParam("name") String name,
      @QueryParam("lang") String language,
      @QueryParam("theme") String theme,
      @QueryParam("density") String density,
      @HeaderParam("Accept-Language") String acceptLanguage) {
    UiContext ui =
        UiContext.resolve(
            language, acceptLanguage, theme, density, "/sns/topic?name=" + encode(name));
    try {
      SnsTopicPort.Topic topic = findTopic(name);
      return Response.ok(
              Templates.subscriptions(topic, topics.listSubscriptions(name), "", "", false, ui)
                  .render(),
              MediaType.TEXT_HTML_TYPE)
          .header("Cache-Control", "no-store")
          .header("X-StackEmu-Fragment", "sns-subscriptions")
          .build();
    } catch (SnsException failure) {
      return error(failure, ui, serviceHref(language, theme, density));
    }
  }

  @POST
  @Path("actions/create")
  public Response createTopic(
      @FormParam("name") String name,
      @FormParam("fifo") boolean fifo,
      @FormParam("lang") String language,
      @FormParam("theme") String theme,
      @FormParam("density") String density) {
    UiContext ui = UiContext.resolve(language, null, theme, density, "/sns");
    try {
      topics.createTopic(name, fifo);
      return redirect(serviceHref(language, theme, density, "topicCreated"));
    } catch (SnsException failure) {
      return page(
          status(failure),
          Templates.create(ui, ui.text(messageKey(failure)), name == null ? "" : name, fifo));
    }
  }

  @POST
  @Path("actions/publish")
  public Response publish(
      @FormParam("topic") String topic,
      @FormParam("message") String message,
      @FormParam("messageGroupId") String messageGroupId,
      @FormParam("messageDeduplicationId") String messageDeduplicationId,
      @FormParam("returnTo") String returnTo,
      @HeaderParam("HX-Request") String htmxRequest,
      @FormParam("lang") String language,
      @FormParam("theme") String theme,
      @FormParam("density") String density) {
    UiContext ui =
        UiContext.resolve(
            language, null, theme, density, topicPage(topic, language, theme, density));
    String origin = actionOrigin(returnTo);
    try {
      topics.publish(topic, message, messageGroupId, messageDeduplicationId);
      if (isHtmx(htmxRequest)) {
        return workspace(
            Templates.actionResult(
                ui.text("snsPublishTitle"),
                ui.text("snsMessagePublished"),
                actionReturnHref(origin, topic, language, theme, density, null),
                ui.text(origin.equals("list") ? "snsBackToTopics" : "snsBackToTopic"),
                "none",
                ui));
      }
      return redirect(
          actionReturnHref(origin, topic, language, theme, density, "messagePublished"));
    } catch (SnsException failure) {
      return actionWorkspace(
          "publish",
          topic,
          origin,
          ui.text(messageKey(failure)),
          message == null ? "" : message,
          "",
          messageGroupId == null ? "" : messageGroupId,
          messageDeduplicationId == null ? "" : messageDeduplicationId,
          ui,
          status(failure));
    }
  }

  @POST
  @Path("actions/subscribe")
  public Response subscribeQueue(
      @FormParam("topic") String topic,
      @FormParam("queue") String queue,
      @FormParam("returnTo") String returnTo,
      @HeaderParam("HX-Request") String htmxRequest,
      @FormParam("lang") String language,
      @FormParam("theme") String theme,
      @FormParam("density") String density) {
    UiContext ui =
        UiContext.resolve(
            language, null, theme, density, topicPage(topic, language, theme, density));
    String origin = actionOrigin(returnTo);
    try {
      topics.subscribeQueue(topic, queue);
      if (isHtmx(htmxRequest)) {
        return workspace(
            Templates.actionResult(
                ui.text("snsSubscribeTitle"),
                ui.text("snsQueueSubscribed"),
                actionReturnHref(origin, topic, language, theme, density, null),
                ui.text(origin.equals("list") ? "snsBackToTopics" : "snsBackToTopic"),
                origin.equals("list") ? "none" : "sns-subscriptions",
                ui));
      }
      return redirect(actionReturnHref(origin, topic, language, theme, density, "queueSubscribed"));
    } catch (SnsException failure) {
      return actionWorkspace(
          "subscribe",
          topic,
          origin,
          ui.text(messageKey(failure)),
          "",
          queue == null ? "" : queue,
          "",
          "",
          ui,
          status(failure));
    } catch (SqsException failure) {
      return actionWorkspace(
          "subscribe",
          topic,
          origin,
          ui.text("snsQueueListUnavailable"),
          "",
          queue == null ? "" : queue,
          "",
          "",
          ui,
          503);
    }
  }

  @POST
  @Path("actions/unsubscribe")
  public Response unsubscribe(
      @FormParam("topic") String topic,
      @FormParam("subscriptionArn") String subscriptionArn,
      @FormParam("confirm") String confirmation,
      @FormParam("lang") String language,
      @FormParam("theme") String theme,
      @FormParam("density") String density) {
    UiContext ui =
        UiContext.resolve(
            language, null, theme, density, topicPage(topic, language, theme, density));
    if (!"yes".equals(confirmation)) {
      return topicActionError(
          400,
          ui.text("snsConfirmationRequired"),
          topic,
          "",
          "",
          "",
          subscriptionArn == null ? "" : subscriptionArn,
          false,
          ui);
    }
    try {
      topics.unsubscribe(topic, subscriptionArn);
      return redirect(topicHref(topic, language, theme, density, "subscriptionRemoved"));
    } catch (SnsException failure) {
      return topicActionError(
          status(failure),
          ui.text(messageKey(failure)),
          topic,
          "",
          "",
          "",
          subscriptionArn == null ? "" : subscriptionArn,
          true,
          ui);
    }
  }

  @POST
  @Path("actions/delete")
  public Response deleteTopic(
      @FormParam("topic") String topic,
      @FormParam("confirmation") String confirmation,
      @FormParam("returnTo") String returnTo,
      @FormParam("lang") String language,
      @FormParam("theme") String theme,
      @FormParam("density") String density) {
    UiContext ui =
        UiContext.resolve(
            language, null, theme, density, topicPage(topic, language, theme, density));
    String origin = actionOrigin(returnTo);
    if (topic == null || !topic.equals(confirmation)) {
      return deleteActionError(
          400, ui.text("snsTopicConfirmationMismatch"), topic, confirmation, origin, ui);
    }
    try {
      topics.deleteTopic(topic);
      return redirect(serviceHref(language, theme, density, "topicDeleted"));
    } catch (SnsException failure) {
      return deleteActionError(
          status(failure), ui.text(messageKey(failure)), topic, confirmation, origin, ui);
    }
  }

  private Response deleteActionError(
      int responseStatus,
      String message,
      String topicName,
      String confirmationDraft,
      String returnTo,
      UiContext ui) {
    try {
      return page(
          responseStatus, deleteReview(topicName, message, confirmationDraft, returnTo, ui));
    } catch (SnsException missingTopic) {
      return error(
          missingTopic,
          ui,
          actionReturnHref(returnTo, topicName, ui.language(), ui.theme(), ui.density(), null));
    }
  }

  private TemplateInstance deleteReview(
      String topicName,
      String actionError,
      String confirmationDraft,
      String returnTo,
      UiContext ui) {
    return Templates.delete(
        findTopic(topicName),
        actionError,
        safe(confirmationDraft),
        returnTo,
        actionReturnHref(returnTo, topicName, ui.language(), ui.theme(), ui.density(), null),
        ui.text(returnTo.equals("list") ? "snsBackToTopics" : "snsBackToTopic"),
        ui);
  }

  private TemplateInstance topicTemplate(String name, String notice, UiContext ui) {
    return topicTemplate(name, notice, "", "", "", "", "", false, ui);
  }

  private TemplateInstance topicTemplate(
      String name,
      String notice,
      String actionError,
      String messageDraft,
      String selectedQueue,
      String confirmationDraft,
      String failedSubscriptionArn,
      boolean unsubscribeConfirmed,
      UiContext ui) {
    return topicTemplate(
        name,
        notice,
        actionError,
        messageDraft,
        selectedQueue,
        confirmationDraft,
        failedSubscriptionArn,
        unsubscribeConfirmed,
        "",
        "",
        ui);
  }

  private Response actionWorkspace(
      String action,
      String topicName,
      String returnTo,
      String actionError,
      String messageDraft,
      String selectedQueue,
      String messageGroupIdDraft,
      String messageDeduplicationIdDraft,
      UiContext ui,
      int responseStatus) {
    try {
      SnsTopicPort.Topic topic = findTopic(topicName);
      List<SqsQueuePort.Queue> availableQueues = List.of();
      String queueMessage = "";
      if ("subscribe".equals(action)) {
        try {
          availableQueues =
              queues.listQueues().stream().filter(queue -> queue.fifo() == topic.fifo()).toList();
        } catch (SqsException failure) {
          queueMessage = ui.text("snsQueueListUnavailable");
        }
      }
      return workspace(
          responseStatus,
          Templates.action(
              topic,
              availableQueues,
              queueMessage,
              action,
              actionError,
              messageDraft,
              selectedQueue,
              messageGroupIdDraft,
              messageDeduplicationIdDraft,
              returnTo,
              actionReturnHref(returnTo, topicName, ui.language(), ui.theme(), ui.density(), null),
              ui.text(returnTo.equals("list") ? "snsBackToTopics" : "snsBackToTopic"),
              ui));
    } catch (SnsException failure) {
      return workspace(
          status(failure),
          Templates.actionUnavailable(
              ui.text(messageKey(failure)),
              actionReturnHref(returnTo, topicName, ui.language(), ui.theme(), ui.density(), null),
              ui));
    }
  }

  private SnsTopicPort.Topic findTopic(String name) {
    return topics.listTopics().stream()
        .filter(candidate -> candidate.name().equals(name))
        .findFirst()
        .orElseThrow(() -> new SnsException(SnsException.Kind.NOT_FOUND));
  }

  private TemplateInstance topicTemplate(
      String name,
      String notice,
      String actionError,
      String messageDraft,
      String selectedQueue,
      String confirmationDraft,
      String failedSubscriptionArn,
      boolean unsubscribeConfirmed,
      String messageGroupIdDraft,
      String messageDeduplicationIdDraft,
      UiContext ui) {
    SnsTopicPort.Topic topic = findTopic(name);
    List<SnsTopicPort.Subscription> subscriptions = topics.listSubscriptions(name);
    List<SqsQueuePort.Queue> availableQueues;
    String queueMessage = "";
    try {
      boolean fifoTopic = topic.fifo();
      availableQueues =
          queues.listQueues().stream().filter(queue -> queue.fifo() == fifoTopic).toList();
    } catch (SqsException failure) {
      availableQueues = List.of();
      queueMessage = ui.text("snsQueueListUnavailable");
    }
    return Templates.topic(
        topic,
        subscriptions,
        availableQueues,
        queueMessage,
        notice,
        actionError,
        messageDraft,
        selectedQueue,
        confirmationDraft,
        failedSubscriptionArn,
        unsubscribeConfirmed,
        messageGroupIdDraft,
        messageDeduplicationIdDraft,
        ui);
  }

  private Response topicActionError(
      int responseStatus,
      String message,
      String topic,
      String messageDraft,
      String selectedQueue,
      String confirmationDraft,
      String failedSubscriptionArn,
      boolean unsubscribeConfirmed,
      UiContext ui) {
    return topicActionError(
        responseStatus,
        message,
        topic,
        messageDraft,
        selectedQueue,
        confirmationDraft,
        failedSubscriptionArn,
        unsubscribeConfirmed,
        "",
        "",
        ui);
  }

  private Response topicActionError(
      int responseStatus,
      String message,
      String topic,
      String messageDraft,
      String selectedQueue,
      String confirmationDraft,
      String failedSubscriptionArn,
      boolean unsubscribeConfirmed,
      String messageGroupIdDraft,
      String messageDeduplicationIdDraft,
      UiContext ui) {
    try {
      return page(
          responseStatus,
          topicTemplate(
              topic,
              "",
              message,
              messageDraft,
              selectedQueue,
              confirmationDraft,
              failedSubscriptionArn,
              unsubscribeConfirmed,
              messageGroupIdDraft,
              messageDeduplicationIdDraft,
              ui));
    } catch (SnsException missingTopic) {
      return error(missingTopic, ui, serviceHref(ui.language(), ui.theme(), ui.density()));
    }
  }

  private Response error(SnsException failure, UiContext ui, String returnHref) {
    return page(status(failure), Templates.error(ui.text(messageKey(failure)), returnHref, ui));
  }

  private Response error(SqsException failure, UiContext ui, String returnHref) {
    String message = ui.text("snsQueueListUnavailable");
    return page(503, Templates.error(message, returnHref, ui));
  }

  private static Response page(TemplateInstance template) {
    return page(Response.Status.OK.getStatusCode(), template);
  }

  private static Response page(int status, TemplateInstance template) {
    return Response.status(status)
        .type(MediaType.TEXT_HTML_TYPE)
        .header("Cache-Control", "no-store")
        .entity(template.render())
        .build();
  }

  private static Response workspace(TemplateInstance template) {
    return workspace(Response.Status.OK.getStatusCode(), template);
  }

  private static Response workspace(int status, TemplateInstance template) {
    return Response.status(status)
        .type(MediaType.TEXT_HTML_TYPE)
        .header("Cache-Control", "no-store")
        .header("X-StackEmu-Action", "workspace")
        .entity(template.render())
        .build();
  }

  private static boolean isHtmx(String htmxRequest) {
    return "true".equals(htmxRequest);
  }

  private static String safe(String value) {
    return value == null ? "" : value;
  }

  private static Response redirect(String href) {
    return Response.seeOther(URI.create(href)).header("Cache-Control", "no-store").build();
  }

  private static int status(SnsException failure) {
    return switch (failure.kind()) {
      case INVALID -> 400;
      case FORBIDDEN -> 403;
      case NOT_FOUND -> 404;
      case CONFLICT -> 409;
      case UNAVAILABLE -> 503;
      case FAILED -> 502;
    };
  }

  private static String messageKey(SnsException failure) {
    return switch (failure.kind()) {
      case INVALID -> "snsInvalidRequest";
      case FORBIDDEN -> "accessDenied";
      case NOT_FOUND -> "snsTopicNotFound";
      case CONFLICT -> "snsTopicConflict";
      case UNAVAILABLE -> "snsUnavailable";
      case FAILED -> "operationFailed";
    };
  }

  private static String notice(String code, UiContext ui) {
    if (code == null) return "";
    return switch (code) {
      case "topicCreated" -> ui.text("snsTopicCreated");
      case "topicDeleted" -> ui.text("snsTopicDeleted");
      case "messagePublished" -> ui.text("snsMessagePublished");
      case "queueSubscribed" -> ui.text("snsQueueSubscribed");
      case "subscriptionRemoved" -> ui.text("snsSubscriptionRemoved");
      default -> "";
    };
  }

  private static String topicPage(String topic, String language, String theme, String density) {
    return "/sns/topic?name="
        + encode(topic)
        + "&lang="
        + encode(language)
        + "&theme="
        + encode(theme)
        + "&density="
        + encode(density);
  }

  private static String topicHref(
      String topic, String language, String theme, String density, String notice) {
    return topicPage(topic, language, theme, density) + noticeQuery(notice);
  }

  private static String actionOrigin(String returnTo) {
    return "list".equals(returnTo) ? "list" : "topic";
  }

  private static String actionReturnHref(
      String returnTo, String topic, String language, String theme, String density, String notice) {
    return "list".equals(returnTo)
        ? serviceHref(language, theme, density, notice)
        : topicHref(topic, language, theme, density, notice);
  }

  private static String serviceHref(String language, String theme, String density) {
    return "/sns" + query(language, theme, density);
  }

  private static String serviceHref(String language, String theme, String density, String notice) {
    return serviceHref(language, theme, density) + noticeQuery(notice);
  }

  private static String query(String language, String theme, String density) {
    return "?lang=" + encode(language) + "&theme=" + encode(theme) + "&density=" + encode(density);
  }

  private static String noticeQuery(String notice) {
    return notice == null ? "" : "&notice=" + encode(notice);
  }

  private static String encode(String value) {
    return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
  }
}
