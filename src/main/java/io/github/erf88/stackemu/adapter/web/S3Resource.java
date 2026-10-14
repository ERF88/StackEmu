package io.github.erf88.stackemu.adapter.web;

import io.github.erf88.stackemu.application.ReadFailure;
import io.github.erf88.stackemu.application.S3ReadPort;
import io.github.erf88.stackemu.application.StreamCopy;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
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
  @Inject S3ReadPort reader;
  @Inject ConnectionView configuration;

  @ConfigProperty(name = "stackemu.page-size", defaultValue = "50")
  int configuredPageSize;

  @CheckedTemplate
  public static class Templates {
    public static native TemplateInstance bucket(
        S3Views.BucketPage view, Ui ui, ConnectionView configuration);

    public static native TemplateInstance object(S3Views.ObjectPage view, Ui ui);

    public static native TemplateInstance error(String message, Ui ui);
  }

  @GET
  @Path("bucket")
  public TemplateInstance bucket(
      @QueryParam("bucket") String bucket,
      @QueryParam("prefix") String requestedPrefix,
      @QueryParam("token") String token,
      @QueryParam("lang") String lang,
      @QueryParam("theme") String theme,
      @HeaderParam("Accept-Language") String accept) {
    String prefix = requestedPrefix == null ? "" : requestedPrefix;
    Ui ui = Ui.resolve(lang, accept, theme, "/");
    try {
      validateBucket(bucket);
      validateKey(prefix, true);
      if (token != null && token.length() > 8192) throw new ReadFailure(ReadFailure.Kind.INVALID);
      ui = Ui.resolve(lang, accept, theme, S3Views.bucketPath(bucket, prefix, token));
      int pageSize = Math.max(1, Math.min(1000, configuredPageSize));
      var details = reader.bucketDetails(bucket);
      var listing =
          reader.listObjects(
              bucket, prefix, token == null || token.isEmpty() ? null : token, pageSize);
      return Templates.bucket(
          S3Views.bucket(details, listing, prefix, pageSize, ui), ui, configuration);
    } catch (ReadFailure failure) {
      throw pageError(failure, ui);
    }
  }

  @GET
  @Path("object")
  public TemplateInstance object(
      @QueryParam("bucket") String bucket,
      @QueryParam("key") String key,
      @QueryParam("lang") String lang,
      @QueryParam("theme") String theme,
      @HeaderParam("Accept-Language") String accept) {
    Ui ui = Ui.resolve(lang, accept, theme, "/");
    try {
      validateBucket(bucket);
      validateKey(key, false);
      ui = Ui.resolve(lang, accept, theme, S3Views.objectPath(bucket, key));
      return Templates.object(S3Views.object(bucket, reader.objectDetails(bucket, key), ui), ui);
    } catch (ReadFailure failure) {
      throw pageError(failure, ui);
    }
  }

  @GET
  @Path("download")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response download(
      @QueryParam("bucket") String bucket,
      @QueryParam("key") String key,
      @QueryParam("lang") String lang,
      @HeaderParam("Accept-Language") String accept) {
    Ui ui = Ui.resolve(lang, accept, null, "/");
    try {
      validateBucket(bucket);
      validateKey(key, false);
      var download = reader.openDownload(bucket, key);
      StreamingOutput body =
          output -> {
            try (download) {
              StreamCopy.copy(download.body(), output);
            } catch (IOException | RuntimeException failure) {
              // Headers may already be committed. Terminate the transfer, never append HTML/error
              // details.
              throw new IOException("Object transfer interrupted");
            }
          };
      var response =
          Response.ok(body, MediaType.APPLICATION_OCTET_STREAM)
              .header("Content-Disposition", S3Views.disposition(key))
              .header("X-Content-Type-Options", "nosniff")
              .header("Cache-Control", "no-store");
      if (download.size() >= 0) response.header("Content-Length", download.size());
      return response.build();
    } catch (ReadFailure failure) {
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
      throw new ReadFailure(ReadFailure.Kind.INVALID);
    }
  }

  private static void validateKey(String key, boolean allowEmpty) {
    if (key == null
        || (!allowEmpty && key.isEmpty())
        || key.getBytes(StandardCharsets.UTF_8).length > 1024) {
      throw new ReadFailure(ReadFailure.Kind.INVALID);
    }
  }

  private static WebApplicationException pageError(ReadFailure failure, Ui ui) {
    return new WebApplicationException(
        Response.status(status(failure.kind()))
            .type(MediaType.TEXT_HTML_TYPE)
            .entity(Templates.error(ui.text(message(failure.kind())), ui).render())
            .build());
  }

  private static int status(ReadFailure.Kind kind) {
    return switch (kind) {
      case NOT_FOUND -> 404;
      case FORBIDDEN -> 403;
      case UNSUPPORTED -> 501;
      case INVALID -> 400;
      case UNAVAILABLE -> 503;
      case FAILED -> 502;
    };
  }

  private static String message(ReadFailure.Kind kind) {
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
