package io.github.erf88.stackemu.adapter.web;

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

@Path("/s3/transfer")
@Blocking
@Produces(MediaType.TEXT_HTML)
public class S3TransferResource {
  private final OperationsResource operations;

  public S3TransferResource(OperationsResource operations) {
    this.operations = operations;
  }

  @CheckedTemplate
  public static class Templates {
    public static native TemplateInstance form(
        String bucket,
        String key,
        boolean move,
        String destinationBucket,
        String destinationKey,
        String message,
        UiContext ui);
  }

  @GET
  public Response form(
      @QueryParam("bucket") String bucket,
      @QueryParam("key") String key,
      @QueryParam("move") boolean move,
      @QueryParam("destinationBucket") String destinationBucket,
      @QueryParam("destinationKey") String destinationKey,
      @QueryParam("lang") String lang,
      @QueryParam("theme") String theme,
      @QueryParam("density") String density) {
    var ui =
        UiContext.resolve(
            lang,
            null,
            theme,
            density,
            "/s3/transfer?bucket="
                + S3ViewMapper.encode(bucket == null ? "" : bucket)
                + "&key="
                + S3ViewMapper.encode(key == null ? "" : key)
                + "&move="
                + move);
    return Response.ok(
            Templates.form(
                    bucket == null ? "" : bucket,
                    key == null ? "" : key,
                    move,
                    destinationBucket == null ? bucket : destinationBucket,
                    destinationKey == null ? key : destinationKey,
                    "",
                    ui)
                .render())
        .header("Cache-Control", "no-store")
        .header("X-StackEmu-Action", "workspace")
        .build();
  }

  @POST
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public Response prepare(
      @RestForm String bucket,
      @RestForm String key,
      @RestForm boolean move,
      @RestForm String destinationBucket,
      @RestForm String destinationKey,
      @RestForm String decision,
      @RestForm String lang,
      @RestForm String theme,
      @RestForm String density) {
    var ui = UiContext.resolve(lang, null, theme, density, "/s3/transfer");
    try {
      return operations.prepare(
          new Request(
              move ? Kind.MOVE : Kind.COPY,
              bucket,
              key,
              "",
              destinationBucket,
              destinationKey,
              "replace".equals(decision)),
          ui);
    } catch (IllegalArgumentException | WriteValidationException invalid) {
      return page(bucket, key, move, "transferInvalid", ui, 422);
    }
  }

  private static Response page(
      String bucket, String key, boolean move, String message, UiContext ui, int status) {
    return Response.status(status)
        .header("Cache-Control", "no-store")
        .header("X-StackEmu-Action", "workspace")
        .entity(Templates.form(bucket, key, move, bucket, key, message, ui).render())
        .build();
  }
}
