package io.github.erf88.stackemu.adapter.web;

import io.github.erf88.stackemu.application.S3ImagePreviewPort;
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

@Path("/s3/preview/image")
@Blocking
public class S3ImagePreviewResource {
  private final S3ImagePreviewPort previewer;

  public S3ImagePreviewResource(S3ImagePreviewPort previewer) {
    this.previewer = previewer;
  }

  public record ImagePreviewPageView(
      String bucket, String key, String contentHref, String downloadHref, String objectHref) {}

  @CheckedTemplate
  public static class Templates {
    public static native TemplateInstance image(ImagePreviewPageView view, UiContext ui);
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  public Response showImagePreview(
      @QueryParam("bucket") String bucket,
      @QueryParam("key") String key,
      @QueryParam("lang") String lang,
      @QueryParam("theme") String theme,
      @QueryParam("density") String density,
      @HeaderParam("Accept-Language") String accept) {
    UiContext ui = UiContext.resolve(lang, accept, theme, density, "/s3");
    try {
      validateTarget(bucket, key);
      ui =
          UiContext.resolve(
              lang, accept, theme, density, S3ViewMapper.imagePreviewPath(bucket, key));
      var view =
          new ImagePreviewPageView(
              bucket,
              key,
              S3ViewMapper.imagePreviewContentPath(bucket, key) + "&" + ui.query(),
              S3ViewMapper.downloadPath(bucket, key) + "&" + ui.query(),
              S3ViewMapper.objectPath(bucket, key) + "&" + ui.query());
      // Shell only: the single img GET owns validation/read/decode. No cached body or second read.
      return Response.ok(Templates.image(view, ui).render())
          .header("Cache-Control", "no-store")
          .build();
    } catch (S3ReadException failure) {
      return Response.status(400)
          .type(MediaType.TEXT_HTML)
          .entity(S3Resource.Templates.error(ui.text("invalidRequest"), ui).render())
          .header("Cache-Control", "no-store")
          .build();
    }
  }

  @GET
  @Path("/content")
  @Produces("image/png")
  public Response imageContent(
      @QueryParam("bucket") String bucket,
      @QueryParam("key") String key,
      @QueryParam("lang") String lang,
      @HeaderParam("Accept-Language") String accept) {
    UiContext ui = UiContext.resolve(lang, accept, null, "/s3");
    try {
      validateTarget(bucket, key);
      var preview = previewer.readImagePreview(bucket, key);
      if (preview.state() != S3ImagePreviewPort.ImageState.AVAILABLE) {
        return switch (preview.state()) {
          case UNSUPPORTED -> refusal(415, "previewImageUnsupported", ui);
          case TOO_LARGE -> refusal(413, "previewImageTooLarge", ui);
          default -> refusal(422, "previewImageInvalid", ui);
        };
      }
      return Response.ok(preview.png(), "image/png")
          .header("Content-Disposition", "inline; filename=\"preview.png\"")
          .header("Cache-Control", "no-store")
          .build();
    } catch (S3ReadException failure) {
      return switch (failure.kind()) {
        case NOT_FOUND -> refusal(404, "resourceNotFound", ui);
        case FORBIDDEN -> refusal(403, "accessDenied", ui);
        case INVALID -> refusal(400, "invalidRequest", ui);
        case UNSUPPORTED -> refusal(501, "unsupportedOperation", ui);
        case UNAVAILABLE -> refusal(503, "unavailable", ui);
        case FAILED -> refusal(502, "operationFailed", ui);
      };
    }
  }

  private static Response refusal(int status, String message, UiContext ui) {
    return Response.status(status)
        .type("text/plain; charset=utf-8")
        .entity(ui.text(message))
        .header("Cache-Control", "no-store")
        .build();
  }

  private static void validateTarget(String bucket, String key) {
    if (bucket == null
        || bucket.length() > 255
        || !bucket.matches("[A-Za-z0-9][A-Za-z0-9.-]*")
        || key == null
        || key.isEmpty()
        || key.getBytes(StandardCharsets.UTF_8).length > 1024)
      throw new S3ReadException(S3ReadException.Kind.INVALID);
  }
}
