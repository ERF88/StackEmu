package io.github.erf88.stackemu.adapter.web;

import io.github.erf88.stackemu.application.S3PreviewPort;
import io.github.erf88.stackemu.application.S3ReadException;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;

@Path("/s3/preview")
@Blocking
@Produces(MediaType.TEXT_HTML)
public class S3PreviewResource {
  private final S3PreviewPort previewer;

  public S3PreviewResource(S3PreviewPort previewer) {
    this.previewer = previewer;
  }

  public record TextPreviewPageView(
      String bucket,
      String key,
      String text,
      String message,
      boolean available,
      String downloadHref,
      String objectHref) {}

  @CheckedTemplate
  public static class Templates {
    public static native TemplateInstance text(TextPreviewPageView view, UiContext ui);
  }

  @GET
  public Response showTextPreview(
      @QueryParam("bucket") String bucket,
      @QueryParam("key") String key,
      @QueryParam("lang") String lang,
      @QueryParam("theme") String theme,
      @HeaderParam("Accept-Language") String accept) {
    UiContext ui = UiContext.resolve(lang, accept, theme, "/");
    try {
      // Same opaque read-target limits as the native detail/download routes.
      if (bucket == null
          || bucket.length() > 255
          || !bucket.matches("[A-Za-z0-9][A-Za-z0-9.-]*")
          || key == null
          || key.isEmpty()
          || key.getBytes(StandardCharsets.UTF_8).length > 1024)
        throw new S3ReadException(S3ReadException.Kind.INVALID);
      ui = UiContext.resolve(lang, accept, theme, S3ViewMapper.previewPath(bucket, key));
      var preview = previewer.readTextPreview(bucket, key);
      String message =
          switch (preview.state()) {
            case AVAILABLE -> "previewTextNote";
            case UNSUPPORTED -> "previewUnsupported";
            case TOO_LARGE -> "previewTooLarge";
            case INVALID_TEXT -> "previewInvalidText";
          };
      var view =
          new TextPreviewPageView(
              bucket,
              key,
              preview.text(),
              ui.text(message),
              preview.state() == S3PreviewPort.PreviewState.AVAILABLE,
              S3ViewMapper.downloadPath(bucket, key) + "&" + ui.query(),
              S3ViewMapper.objectPath(bucket, key) + "&" + ui.query());
      return Response.ok(Templates.text(view, ui).render())
          .header("Cache-Control", "no-store")
          .build();
    } catch (S3ReadException failure) {
      int status =
          switch (failure.kind()) {
            case NOT_FOUND -> 404;
            case FORBIDDEN -> 403;
            case INVALID -> 400;
            case UNSUPPORTED -> 501;
            case UNAVAILABLE -> 503;
            case FAILED -> 502;
          };
      String message =
          switch (failure.kind()) {
            case NOT_FOUND -> "resourceNotFound";
            case FORBIDDEN -> "accessDenied";
            case INVALID -> "invalidRequest";
            case UNSUPPORTED -> "unsupportedOperation";
            case UNAVAILABLE -> "unavailable";
            case FAILED -> "operationFailed";
          };
      return Response.status(status)
          .entity(S3Resource.Templates.error(ui.text(message), ui).render())
          .header("Cache-Control", "no-store")
          .build();
    }
  }
}
