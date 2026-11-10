package io.github.erf88.stackemu.adapter.web.sqs;

import io.github.erf88.stackemu.adapter.web.UiContext;
import io.github.erf88.stackemu.application.sqs.SqsException;
import io.github.erf88.stackemu.application.sqs.SqsQueuePort;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

/** Read-only SQS navigation. Mutations remain separate form endpoints. */
@Path("/sqs")
@Blocking
@Produces(MediaType.TEXT_HTML)
public class SqsResource {
  private final SqsQueuePort queues;

  public SqsResource(SqsQueuePort queues) {
    this.queues = queues;
  }

  @CheckedTemplate
  public static class Templates {
    public static native TemplateInstance index(List<SqsQueuePort.Queue> queues, UiContext ui);

    public static native TemplateInstance queue(SqsQueuePort.Queue queue, UiContext ui);

    public static native TemplateInstance error(String message, UiContext ui);
  }

  @GET
  public TemplateInstance list(
      @QueryParam("lang") String language,
      @QueryParam("theme") String theme,
      @QueryParam("density") String density,
      @HeaderParam("Accept-Language") String acceptLanguage) {
    UiContext ui = UiContext.resolve(language, acceptLanguage, theme, density, "/sqs");
    try {
      return Templates.index(queues.listQueues(), ui);
    } catch (SqsException failure) {
      throw pageError(failure, ui);
    }
  }

  @GET
  @Path("queues")
  public Response queueList(
      @QueryParam("lang") String language,
      @QueryParam("theme") String theme,
      @QueryParam("density") String density,
      @HeaderParam("Accept-Language") String acceptLanguage) {
    UiContext ui = UiContext.resolve(language, acceptLanguage, theme, density, "/sqs");
    try {
      return Response.ok(Templates.index(queues.listQueues(), ui).render())
          .type(MediaType.TEXT_HTML_TYPE)
          .header("Cache-Control", "no-store")
          .header("X-StackEmu-Fragment", "queue-list")
          .build();
    } catch (SqsException failure) {
      throw pageError(failure, ui);
    }
  }

  @GET
  @Path("queue")
  public TemplateInstance queue(
      @QueryParam("name") String name,
      @QueryParam("lang") String language,
      @QueryParam("theme") String theme,
      @QueryParam("density") String density,
      @HeaderParam("Accept-Language") String acceptLanguage) {
    UiContext ui =
        UiContext.resolve(
            language, acceptLanguage, theme, density, "/sqs/queue?name=" + encode(name));
    try {
      return Templates.queue(queues.getQueue(name), ui);
    } catch (SqsException failure) {
      throw pageError(failure, ui);
    }
  }

  private static WebApplicationException pageError(SqsException failure, UiContext ui) {
    return new WebApplicationException(
        Response.status(SqsWebSupport.status(failure))
            .type(MediaType.TEXT_HTML_TYPE)
            .header("Cache-Control", "no-store")
            .entity(Templates.error(ui.text(SqsWebSupport.messageKey(failure)), ui).render())
            .build());
  }

  private static String encode(String value) {
    return java.net.URLEncoder.encode(
        value == null ? "" : value, java.nio.charset.StandardCharsets.UTF_8);
  }
}
