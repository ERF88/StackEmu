package io.github.erf88.stackemu.adapter.web;

import io.github.erf88.stackemu.application.ObjectDeletionException;
import io.github.erf88.stackemu.application.S3ObjectDeletionPort;
import io.github.erf88.stackemu.application.S3ObjectDeletionPort.Target;
import io.github.erf88.stackemu.application.WriteValidationException;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;

@Path("/s3/delete-object")
@Blocking
@Produces(MediaType.TEXT_HTML)
public class S3ObjectDeletionResource {
  private final S3ObjectDeletionPort deletion;
  private final ObjectDeletionConfirmations confirmations;

  public S3ObjectDeletionResource(
      S3ObjectDeletionPort deletion, ObjectDeletionConfirmations confirmations) {
    this.deletion = deletion;
    this.confirmations = confirmations;
  }

  public record DeletionPageView(
      String bucket, String key, String etag, String ticket, String message, String backHref) {}

  @CheckedTemplate
  public static class Templates {
    public static native TemplateInstance confirmation(DeletionPageView view, UiContext ui);
  }

  @GET
  public Response showConfirmation(
      @QueryParam("bucket") String bucket,
      @QueryParam("key") String key,
      @QueryParam("lang") String lang,
      @QueryParam("theme") String theme,
      @QueryParam("density") String density,
      @HeaderParam("Accept-Language") String acceptLanguage) {
    String path =
        "/s3/delete-object?bucket="
            + S3ViewMapper.encode(value(bucket))
            + "&key="
            + S3ViewMapper.encode(value(key));
    UiContext ui = UiContext.resolve(lang, acceptLanguage, theme, density, path);
    try {
      Target target = deletion.inspectObject(bucket, key);
      var ticket = confirmations.issue(target);
      return page(
          ticket.isPresent() ? 200 : 429,
          target,
          ticket.orElse(""),
          ticket.isPresent() ? "" : "deleteCapacity",
          ui);
    } catch (WriteValidationException invalid) {
      return page(422, null, "", "deleteUnsafe", ui);
    } catch (ObjectDeletionException failure) {
      return failure(failure, null, ui);
    }
  }

  @POST
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public Response deleteObject(
      @RestForm String ticket,
      @RestForm String confirm,
      @RestForm String lang,
      @RestForm String theme,
      @RestForm String density) {
    UiContext ui = UiContext.resolve(lang, null, theme, density, "/s3");
    // The target/ETag are server-held; extra bucket/key/ETag form fields are never consulted.
    Target target = confirmations.consume(ticket).orElse(null);
    if (target == null) return page(409, null, "", "deleteExpired", ui);
    if (!"yes".equals(confirm)) return page(422, target, "", "confirmationRequired", ui);
    try {
      deletion.deleteObject(target);
      return page(200, target, "", "deleteCompleted", ui);
    } catch (ObjectDeletionException failure) {
      return failure(failure, target, ui);
    }
  }

  private Response failure(ObjectDeletionException failure, Target target, UiContext ui) {
    int status =
        switch (failure.kind()) {
          case UNSAFE, CONFLICT -> 409;
          case NOT_FOUND -> 404;
          case FORBIDDEN -> 403;
          case UNAVAILABLE -> 503;
          case UNCERTAIN -> 502;
        };
    String message =
        switch (failure.kind()) {
          case UNSAFE -> "deleteUnsafe";
          case CONFLICT -> "deleteConflict";
          case NOT_FOUND -> "deleteMissing";
          case FORBIDDEN -> "deleteForbidden";
          case UNAVAILABLE -> "deleteUnavailable";
          case UNCERTAIN -> "deleteUncertain";
        };
    return page(status, target, "", message, ui);
  }

  private Response page(int status, Target target, String ticket, String message, UiContext ui) {
    String back = "/s3?" + ui.query();
    if (target != null) {
      int slash = target.key().lastIndexOf('/');
      String prefix = slash < 0 ? "" : target.key().substring(0, slash + 1);
      back = S3ViewMapper.bucketPath(target.bucket(), prefix, null) + "&" + ui.query();
    }
    return Response.status(status)
        .type(MediaType.TEXT_HTML_TYPE)
        .header("Cache-Control", "no-store")
        .entity(
            Templates.confirmation(
                    new DeletionPageView(
                        target == null ? "" : target.bucket(),
                        target == null ? "" : target.key(),
                        target == null ? "" : target.etag(),
                        ticket,
                        message,
                        back),
                    ui)
                .render())
        .build();
  }

  private static String value(String value) {
    return value == null ? "" : value;
  }
}
