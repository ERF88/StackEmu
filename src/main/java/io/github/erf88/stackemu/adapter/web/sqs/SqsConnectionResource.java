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
import jakarta.ws.rs.core.MediaType;

/** Contextual SQS diagnostic. It only probes SQS. */
@Path("/sqs/connection")
@Blocking
@Produces(MediaType.TEXT_HTML)
public class SqsConnectionResource {
  private final SqsQueuePort queues;
  private final SqsConnectionSettingsView configuration;

  public SqsConnectionResource(SqsQueuePort queues, SqsConnectionSettingsView configuration) {
    this.queues = queues;
    this.configuration = configuration;
  }

  @CheckedTemplate
  public static class Templates {
    public static native TemplateInstance connection(
        boolean connected, UiContext ui, SqsConnectionSettingsView configuration);
  }

  @GET
  public TemplateInstance connection(
      @QueryParam("lang") String language,
      @QueryParam("theme") String theme,
      @QueryParam("density") String density,
      @HeaderParam("Accept-Language") String acceptLanguage) {
    UiContext ui = UiContext.resolve(language, acceptLanguage, theme, density, "/sqs/connection");
    try {
      queues.listQueues();
      return Templates.connection(true, ui, configuration);
    } catch (SqsException ignored) {
      return Templates.connection(false, ui, configuration);
    }
  }
}
