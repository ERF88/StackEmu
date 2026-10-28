package io.github.erf88.stackemu.adapter.web;

import io.github.erf88.stackemu.application.OperationException;
import io.github.erf88.stackemu.application.S3ObjectToolsPort;
import io.github.erf88.stackemu.application.S3OperationPort.Kind;
import io.github.erf88.stackemu.application.S3OperationPort.Request;
import io.github.erf88.stackemu.application.WriteValidationException;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;

/** Preserves the canonical action URL while using the durable reviewed workflow. */
@Path("/s3/delete-object")
@Blocking
@Produces(MediaType.TEXT_HTML)
public class S3ObjectDeletionResource {
  private final OperationsResource operations;
  private final S3ObjectToolsPort tools;

  public S3ObjectDeletionResource(OperationsResource operations, S3ObjectToolsPort tools) {
    this.operations = operations;
    this.tools = tools;
  }

  @CheckedTemplate
  public static class Templates {
    public static native TemplateInstance unavailable(
        String bucket, String key, String message, UiContext ui);
  }

  private Response unavailable(String bucket, String key, String message, UiContext ui) {
    return Response.status(409)
        .header("Cache-Control", "no-store")
        .header("X-StackEmu-Action", "workspace")
        .entity(Templates.unavailable(bucket, key, message, ui).render())
        .build();
  }

  @GET
  public Response showConfirmation(
      @QueryParam("bucket") String bucket,
      @QueryParam("key") String key,
      @QueryParam("version") String version,
      @QueryParam("lang") String lang,
      @QueryParam("theme") String theme,
      @QueryParam("density") String density) {
    var ui = UiContext.resolve(lang, null, theme, density, "/s3/delete-object");
    try {
      Kind kind =
          version != null && !version.isEmpty()
              ? Kind.DELETE_VERSION
              : key != null && key.endsWith("/") ? Kind.DELETE_PREFIX : Kind.DELETE_OBJECT;
      var request = new Request(kind, bucket, key, version, "", "", false);
      if (kind == Kind.DELETE_VERSION && !tools.isVersionDeletionAvailable(bucket))
        return unavailable(bucket, key, "versionDeletionUnavailable", ui);
      return operations.prepare(request, ui);
    } catch (OperationException failure) {
      return unavailable(bucket, key, "operationReason" + failure.reason().name(), ui);
    } catch (IllegalArgumentException | WriteValidationException invalid) {
      return operations.invalid(ui);
    }
  }

  @POST
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public Response submit(
      @RestForm String ticket,
      @RestForm String bucketName,
      @RestForm String lang,
      @RestForm String theme,
      @RestForm String density) {
    return operations.start(ticket, bucketName, lang, theme, density);
  }
}
