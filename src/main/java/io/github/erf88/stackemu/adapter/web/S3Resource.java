package io.github.erf88.stackemu.adapter.web;

import io.github.erf88.stackemu.application.S3ReadException;
import io.github.erf88.stackemu.application.S3ReadPort;
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
import jakarta.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/s3")
@Blocking
@Produces(MediaType.TEXT_HTML)
public class S3Resource {
  private final S3ReadPort reader;

  public S3Resource(S3ReadPort reader) {
    this.reader = reader;
  }

  @ConfigProperty(name = "stackemu.page-size", defaultValue = "50")
  int configuredPageSize;

  @CheckedTemplate
  public static class Templates {
    public static native TemplateInstance bucket(S3ViewMapper.BucketPageView view, UiContext ui);

    public static native TemplateInstance object(S3ViewMapper.ObjectPageView view, UiContext ui);

    public static native TemplateInstance metadata(S3ViewMapper.ObjectPageView view, UiContext ui);

    public static native TemplateInstance objectPanel(
        S3ViewMapper.ObjectPageView view, UiContext ui);

    public static native TemplateInstance error(String message, UiContext ui);
  }

  @GET
  @Path("bucket")
  public Response showBucket(
      @QueryParam("bucket") String bucket,
      @QueryParam("prefix") String requestedPrefix,
      @QueryParam("token") String token,
      @QueryParam("lang") String lang,
      @QueryParam("theme") String theme,
      @QueryParam("density") String density,
      @HeaderParam("Accept-Language") String accept) {
    String prefix = requestedPrefix == null ? "" : requestedPrefix;
    UiContext ui = UiContext.resolve(lang, accept, theme, density, "/s3");
    try {
      validateBucket(bucket);
      validateKey(prefix, true);
      if (token != null && token.length() > 8192)
        throw new S3ReadException(S3ReadException.Kind.INVALID);
      ui =
          UiContext.resolve(
              lang, accept, theme, density, S3ViewMapper.bucketPath(bucket, prefix, token));
      int pageSize = Math.max(1, Math.min(1000, configuredPageSize));
      var listing =
          reader.listObjects(
              bucket, prefix, token == null || token.isEmpty() ? null : token, pageSize);
      return Response.ok(
              Templates.bucket(S3ViewMapper.toBucketPage(bucket, listing, prefix, token, ui), ui)
                  .render())
          .header("X-StackEmu-Fragment", "object-list")
          .header("Cache-Control", "no-store")
          .build();
    } catch (S3ReadException failure) {
      throw pageError(failure, ui);
    }
  }

  @GET
  @Path("object")
  public TemplateInstance showObject(
      @QueryParam("bucket") String bucket,
      @QueryParam("key") String key,
      @QueryParam("lang") String lang,
      @QueryParam("theme") String theme,
      @QueryParam("density") String density,
      @HeaderParam("Accept-Language") String accept) {
    UiContext ui = UiContext.resolve(lang, accept, theme, density, "/s3");
    try {
      validateBucket(bucket);
      validateKey(key, false);
      ui = UiContext.resolve(lang, accept, theme, density, S3ViewMapper.objectPath(bucket, key));
      return Templates.object(
          S3ViewMapper.toObjectPage(bucket, reader.getObjectDetails(bucket, key), ui), ui);
    } catch (S3ReadException failure) {
      throw pageError(failure, ui);
    }
  }

  @GET
  @Path("object-metadata")
  public Response showMetadata(
      @QueryParam("bucket") String bucket,
      @QueryParam("key") String key,
      @QueryParam("lang") String lang,
      @QueryParam("theme") String theme,
      @QueryParam("density") String density) {
    var ui =
        UiContext.resolve(
            lang,
            null,
            theme,
            density,
            "/s3/object-metadata?bucket="
                + S3ViewMapper.encode(bucket == null ? "" : bucket)
                + "&key="
                + S3ViewMapper.encode(key == null ? "" : key));
    try {
      validateBucket(bucket);
      validateKey(key, false);
      return Response.ok(
              Templates.metadata(
                      S3ViewMapper.toObjectPage(bucket, reader.getObjectDetails(bucket, key), ui),
                      ui)
                  .render())
          .header("X-StackEmu-Fragment", "object-inspector")
          .header("Cache-Control", "no-store")
          .build();
    } catch (S3ReadException failure) {
      throw pageError(failure, ui);
    }
  }

  @GET
  @Path("object-panel")
  public Response showObjectPanel(
      @QueryParam("bucket") String bucket,
      @QueryParam("key") String key,
      @QueryParam("lang") String lang,
      @QueryParam("theme") String theme,
      @QueryParam("density") String density,
      @HeaderParam("Accept-Language") String accept,
      @HeaderParam("HX-Request") String hxRequest) {
    UiContext ui = UiContext.resolve(lang, accept, theme, density, "/s3");
    try {
      validateBucket(bucket);
      validateKey(key, false);
      ui = UiContext.resolve(lang, accept, theme, density, S3ViewMapper.objectPath(bucket, key));
      var view = S3ViewMapper.toObjectPage(bucket, reader.getObjectDetails(bucket, key), ui);
      boolean fragment = "true".equals(hxRequest);
      var response =
          Response.ok(
              (fragment ? Templates.objectPanel(view, ui) : Templates.object(view, ui)).render());
      if (fragment) response.header("X-StackEmu-Fragment", "object-details");
      return response.header("Cache-Control", "no-store").header("Vary", "HX-Request").build();
    } catch (S3ReadException failure) {
      return Response.status(status(failure.kind()))
          .type(MediaType.TEXT_PLAIN_TYPE)
          .entity(ui.text(message(failure.kind())))
          .header("Cache-Control", "no-store")
          .header("Vary", "HX-Request")
          .build();
    }
  }

  @GET
  @Path("download")
  @Produces(MediaType.WILDCARD)
  public Response downloadObject(
      @QueryParam("bucket") String bucket,
      @QueryParam("key") String key,
      @QueryParam("lang") String lang,
      @HeaderParam("Accept-Language") String accept) {
    UiContext ui = UiContext.resolve(lang, accept, null, "/s3");
    try {
      validateBucket(bucket);
      validateKey(key, false);
      var download = reader.openDownload(bucket, key);
      StreamingOutput body =
          output -> {
            try (download) {
              DownloadStreamCopier.copy(download.body(), output);
            } catch (IOException | RuntimeException failure) {
              // Headers may already be committed. Terminate the transfer, never append HTML/error
              // details.
              throw new IOException("Object transfer interrupted");
            }
          };
      var response =
          Response.ok(body, FileContentTypes.safeContentType(download.contentType()))
              .header("Content-Disposition", DownloadHeaders.attachmentDisposition(key))
              .header("X-Content-Type-Options", "nosniff")
              .header("Cache-Control", "no-store");
      if (download.size() >= 0) response.header("Content-Length", download.size());
      return response.build();
    } catch (S3ReadException failure) {
      return Response.status(status(failure.kind()))
          .type(MediaType.TEXT_PLAIN_TYPE)
          .entity(ui.text(message(failure.kind())))
          .header("X-Content-Type-Options", "nosniff")
          .header("Cache-Control", "no-store")
          .build();
    }
  }

  private static void validateBucket(String bucket) {
    if (bucket == null
        || bucket.isEmpty()
        || bucket.length() > 255
        || !bucket.matches("[A-Za-z0-9][A-Za-z0-9.-]*")) {
      throw new S3ReadException(S3ReadException.Kind.INVALID);
    }
  }

  private static void validateKey(String key, boolean allowEmpty) {
    if (key == null
        || (!allowEmpty && key.isEmpty())
        || key.getBytes(StandardCharsets.UTF_8).length > 1024) {
      throw new S3ReadException(S3ReadException.Kind.INVALID);
    }
  }

  private static WebApplicationException pageError(S3ReadException failure, UiContext ui) {
    return new WebApplicationException(
        Response.status(status(failure.kind()))
            .type(MediaType.TEXT_HTML_TYPE)
            .entity(Templates.error(ui.text(message(failure.kind())), ui).render())
            .build());
  }

  private static int status(S3ReadException.Kind kind) {
    return switch (kind) {
      case NOT_FOUND -> 404;
      case FORBIDDEN -> 403;
      case UNSUPPORTED -> 501;
      case INVALID -> 400;
      case UNAVAILABLE -> 503;
      case FAILED -> 502;
    };
  }

  private static String message(S3ReadException.Kind kind) {
    return switch (kind) {
      case NOT_FOUND -> "resourceNotFound";
      case FORBIDDEN -> "accessDenied";
      case UNSUPPORTED -> "unsupportedOperation";
      case INVALID -> "invalidRequest";
      case UNAVAILABLE -> "unavailable";
      case FAILED -> "operationFailed";
    };
  }
}
