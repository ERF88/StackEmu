package io.github.erf88.stackemu.adapter.web;

import io.github.erf88.stackemu.application.S3OperationPort.Kind;
import io.github.erf88.stackemu.application.S3OperationPort.Request;
import io.github.erf88.stackemu.application.WriteValidationException;
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
@Path("/s3/empty-bucket")
@Blocking
@Produces(MediaType.TEXT_HTML)
public class S3BucketCleanupResource {
  private final OperationsResource operations;

  public S3BucketCleanupResource(OperationsResource operations) {
    this.operations = operations;
  }

  @GET
  public Response showConfirmation(
      @QueryParam("bucket") String bucket,
      @QueryParam("key") String key,
      @QueryParam("version") String version,
      @QueryParam("lang") String lang,
      @QueryParam("theme") String theme,
      @QueryParam("density") String density) {
    var ui = UiContext.resolve(lang, null, theme, density, "/s3/empty-bucket");
    try {
      Kind kind = Kind.EMPTY_BUCKET;
      return operations.prepare(new Request(kind, bucket, "", "", "", "", false), ui);
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
