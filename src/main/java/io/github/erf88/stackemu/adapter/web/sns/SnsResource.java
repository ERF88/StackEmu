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

    public static native TemplateInstance topic(
        SnsTopicPort.Topic topic,
        List<SnsTopicPort.Subscription> subscriptions,
        List<SqsQueuePort.Queue> queues,
        String queueMessage,
        String notice,
        UiContext ui);

    public static native TemplateInstance error(String message, String returnHref, UiContext ui);
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

  @POST
  @Path("actions/create")
  public Response createTopic(
      @FormParam("name") String name,
      @FormParam("lang") String language,
      @FormParam("theme") String theme,
      @FormParam("density") String density) {
    UiContext ui = UiContext.resolve(language, null, theme, density, "/sns");
    try {
      topics.createTopic(name);
      return redirect(serviceHref(language, theme, density, "topicCreated"));
    } catch (SnsException failure) {
      return error(failure, ui, serviceHref(language, theme, density));
    }
  }

  @POST
  @Path("actions/publish")
  public Response publish(
      @FormParam("topic") String topic,
      @FormParam("message") String message,
      @FormParam("lang") String language,
      @FormParam("theme") String theme,
      @FormParam("density") String density) {
    UiContext ui =
        UiContext.resolve(
            language, null, theme, density, topicPage(topic, language, theme, density));
    try {
      topics.publish(topic, message);
      return redirect(topicHref(topic, language, theme, density, "messagePublished"));
    } catch (SnsException failure) {
      return error(failure, ui, topicHref(topic, language, theme, density, null));
    }
  }

  @POST
  @Path("actions/subscribe")
  public Response subscribeQueue(
      @FormParam("topic") String topic,
      @FormParam("queue") String queue,
      @FormParam("lang") String language,
      @FormParam("theme") String theme,
      @FormParam("density") String density) {
    UiContext ui =
        UiContext.resolve(
            language, null, theme, density, topicPage(topic, language, theme, density));
    try {
      topics.subscribeQueue(topic, queue);
      return redirect(topicHref(topic, language, theme, density, "queueSubscribed"));
    } catch (SnsException failure) {
      return error(failure, ui, topicHref(topic, language, theme, density, null));
    } catch (SqsException failure) {
      return error(failure, ui, topicHref(topic, language, theme, density, null));
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
      return page(
          400,
          Templates.error(
              ui.text("snsConfirmationRequired"),
              topicHref(topic, language, theme, density, null),
              ui));
    }
    try {
      topics.unsubscribe(topic, subscriptionArn);
      return redirect(topicHref(topic, language, theme, density, "subscriptionRemoved"));
    } catch (SnsException failure) {
      return error(failure, ui, topicHref(topic, language, theme, density, null));
    }
  }

  @POST
  @Path("actions/delete")
  public Response deleteTopic(
      @FormParam("topic") String topic,
      @FormParam("confirmation") String confirmation,
      @FormParam("lang") String language,
      @FormParam("theme") String theme,
      @FormParam("density") String density) {
    UiContext ui =
        UiContext.resolve(
            language, null, theme, density, topicPage(topic, language, theme, density));
    if (topic == null || !topic.equals(confirmation)) {
      return page(
          400,
          Templates.error(
              ui.text("snsTopicConfirmationMismatch"),
              topicHref(topic, language, theme, density, null),
              ui));
    }
    try {
      topics.deleteTopic(topic);
      return redirect(serviceHref(language, theme, density, "topicDeleted"));
    } catch (SnsException failure) {
      return error(failure, ui, topicHref(topic, language, theme, density, null));
    }
  }

  private TemplateInstance topicTemplate(String name, String notice, UiContext ui) {
    SnsTopicPort.Topic topic =
        topics.listTopics().stream()
            .filter(candidate -> candidate.name().equals(name))
            .findFirst()
            .orElseThrow(() -> new SnsException(SnsException.Kind.NOT_FOUND));
    List<SnsTopicPort.Subscription> subscriptions = topics.listSubscriptions(name);
    List<SqsQueuePort.Queue> availableQueues;
    String queueMessage = "";
    try {
      boolean fifoTopic = topic.name().endsWith(".fifo");
      availableQueues =
          queues.listQueues().stream().filter(queue -> queue.fifo() == fifoTopic).toList();
    } catch (SqsException failure) {
      availableQueues = List.of();
      queueMessage = ui.text("snsQueueListUnavailable");
    }
    return Templates.topic(topic, subscriptions, availableQueues, queueMessage, notice, ui);
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
