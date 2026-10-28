package io.github.erf88.stackemu.adapter.web;

import io.github.erf88.stackemu.application.OperationException;
import io.github.erf88.stackemu.application.S3ObjectToolsPort;
import io.github.erf88.stackemu.application.S3WriteException;
import io.github.erf88.stackemu.application.S3WritePort;
import io.github.erf88.stackemu.application.S3WriteValidation;
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
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

@Path("/s3/write")
@Blocking
@Produces(MediaType.TEXT_HTML)
public class S3WriteResource {
  private final S3WritePort writer;
  private final ConnectionSettingsView configuration;
  private final long maximumUpload;
  private final S3ObjectToolsPort tools;
  private final UploadReplacementConfirmations replacements;

  public S3WriteResource(
      S3WritePort writer,
      ConnectionSettingsView configuration,
      S3ObjectToolsPort tools,
      UploadReplacementConfirmations replacements,
      @ConfigProperty(name = "stackemu.max-upload") long maximumUpload) {
    if (maximumUpload < 1) {
      throw new IllegalArgumentException("Upload limit must be positive");
    }
    this.writer = writer;
    this.configuration = configuration;
    this.maximumUpload = maximumUpload;
    this.tools = tools;
    this.replacements = replacements;
  }

  public record MutationPageView(
      String action,
      String bucket,
      String key,
      String message,
      Map<String, String> errors,
      String command,
      long maximumUpload) {
    public MutationPageView {
      errors = Map.copyOf(errors);
    }

    public boolean createBucket() {
      return action.equals("bucket");
    }

    public boolean upload() {
      return action.equals("upload");
    }

    public String bucketHref() {
      return S3ViewMapper.bucketPath(bucket, key, null);
    }

    public String resultBackHref() {
      String prefix = upload() ? key.substring(0, key.lastIndexOf('/') + 1) : key;
      return S3ViewMapper.bucketPath(bucket, prefix, null);
    }
  }

  @CheckedTemplate
  public static class Templates {
    public static native TemplateInstance form(MutationPageView view, UiContext ui);

    public static native TemplateInstance result(
        MutationPageView view, UiContext ui, String destination);
  }

  @GET
  public Response showMutationForm(
      @QueryParam("action") String action,
      @QueryParam("bucket") String bucket,
      @QueryParam("prefix") String prefix,
      @QueryParam("lang") String language,
      @QueryParam("theme") String theme,
      @QueryParam("density") String density,
      @HeaderParam("Accept-Language") String acceptLanguage) {
    String selected = "bucket".equals(action) || "folder".equals(action) ? action : "upload";
    return Response.ok(
            form(
                    selected,
                    value(bucket),
                    value(prefix),
                    "",
                    Map.of(),
                    UiContext.resolve(
                        language,
                        acceptLanguage,
                        theme,
                        density,
                        formPath(selected, bucket, prefix)))
                .render())
        .header("Cache-Control", "no-store")
        .header("X-StackEmu-Action", "workspace")
        .build();
  }

  @POST
  @Path("bucket")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public Response createBucket(
      @RestForm String bucket,
      @RestForm String confirm,
      @RestForm String lang,
      @RestForm String theme,
      @RestForm String density) {
    return mutate("bucket", bucket, "", confirm, null, lang, theme, density);
  }

  @POST
  @Path("folder")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public Response createFolderMarker(
      @RestForm String bucket,
      @RestForm String key,
      @RestForm String confirm,
      @RestForm String lang,
      @RestForm String theme,
      @RestForm String density) {
    return mutate("folder", bucket, key, confirm, null, lang, theme, density);
  }

  @POST
  @Path("upload")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response uploadObject(
      @RestForm String bucket,
      @RestForm String prefix,
      @RestForm String confirm,
      @RestForm FileUpload file,
      @RestForm String replacementTicket,
      @RestForm String decision,
      @RestForm String filename,
      @RestForm String lang,
      @RestForm String theme,
      @RestForm String density)
      throws IOException {
    try {
      return Response.fromResponse(
              mutate(
                  "upload",
                  bucket,
                  prefix,
                  confirm,
                  file,
                  lang,
                  theme,
                  density,
                  replacementTicket,
                  decision,
                  filename))
          .header("X-StackEmu-Upload", "workspace")
          .build();
    } finally {
      // The name comes from Quarkus, never from the client-supplied filename or key.
      if (file != null) {
        Files.deleteIfExists(file.uploadedFile());
      }
    }
  }

  private Response mutate(
      String action,
      String bucket,
      String key,
      String confirm,
      FileUpload file,
      String language,
      String theme,
      String density) {
    return mutate(action, bucket, key, confirm, file, language, theme, density, null, null, null);
  }

  private Response mutate(
      String action,
      String bucket,
      String key,
      String confirm,
      FileUpload file,
      String language,
      String theme,
      String density,
      String replacementTicket,
      String decision,
      String filename) {
    UiContext ui = UiContext.resolve(language, null, theme, density, formPath(action, bucket, key));
    try {
      if (!"yes".equals(confirm)) {
        throw new WriteValidationException("confirm", "confirmationRequired");
      }
      S3WriteValidation.validateBucketName(bucket);
      String target = key;
      String contentType = "application/octet-stream";
      switch (action) {
        case "bucket" -> writer.createBucket(bucket);
        case "folder" -> {
          target = S3WriteValidation.folderMarkerKey(key);
          writer.createFolderMarker(bucket, target);
        }
        default -> {
          if (file == null) {
            throw new WriteValidationException("file", "fileRequired");
          }
          String original = UploadFileNames.originalFilename(file.fileName());
          String selected = filename == null || filename.isBlank() ? original : filename;
          int extension = original.lastIndexOf('.');
          if (extension > 0 && !selected.endsWith(original.substring(extension)))
            throw new WriteValidationException("file", "invalidFilename");
          target = S3WriteValidation.uploadObjectKey(key, selected);
          S3WriteValidation.validateUploadSize(Files.size(file.uploadedFile()), maximumUpload);
          contentType = FileContentTypes.uploadContentType(target, file.contentType());
          if ("replace".equals(decision)) {
            var reviewed =
                replacements
                    .consume(replacementTicket)
                    .orElseThrow(
                        () -> new WriteValidationException("file", "replacementReviewRequired"));
            if (!reviewed.bucket().equals(bucket) || !reviewed.key().equals(target))
              throw new WriteValidationException("file", "replacementReviewRequired");
            tools.replaceUpload(bucket, target, reviewed.etag(), file.uploadedFile(), contentType);
          } else writer.uploadObject(bucket, target, file.uploadedFile(), contentType);
        }
      }
      String destination =
          action.equals("upload")
              ? S3ViewMapper.objectPath(bucket, target)
              : S3ViewMapper.bucketPath(bucket, action.equals("folder") ? target : "", null);
      String command =
          S3CliCommands.equivalent(
              action,
              configuration.endpoint(),
              configuration.region(),
              bucket,
              target,
              contentType);
      return Response.ok(
              Templates.result(
                      new MutationPageView(
                          action, bucket, target, "", Map.of(), command, maximumUpload),
                      ui,
                      destination + "&" + ui.query())
                  .render())
          .header("Cache-Control", "no-store")
          .header("X-StackEmu-Action", "workspace")
          .build();
    } catch (WriteValidationException invalid) {
      return page(422, action, bucket, key, "", invalid.errors(), ui);
    } catch (S3WriteException failure) {
      if (action.equals("upload")
          && file != null
          && failure.kind() == S3WriteException.Kind.CONFLICT) {
        try {
          String selected =
              filename == null || filename.isBlank()
                  ? UploadFileNames.originalFilename(file.fileName())
                  : filename;
          String target = S3WriteValidation.uploadObjectKey(key, selected);
          String replacement =
              replacements.issue(bucket, target, tools.destinationEtag(bucket, target));
          return page(
              409,
              action,
              bucket,
              key,
              "uploadDestinationExists",
              Map.of("replacementTicket", replacement, "replacementName", selected),
              ui);
        } catch (OperationException unavailable) {
          return page(
              409,
              action,
              bucket,
              key,
              "operationReason" + unavailable.reason().name(),
              Map.of(),
              ui);
        }
      }
      int status =
          switch (failure.kind()) {
            case CONFLICT -> 409;
            case NOT_FOUND -> 404;
            case FORBIDDEN -> 403;
            case UNAVAILABLE -> 503;
            case FAILED -> 502;
          };
      return page(
          status,
          action,
          bucket,
          key,
          failure.kind() == S3WriteException.Kind.CONFLICT ? "writeConflict" : "operationFailed",
          Map.of(),
          ui);
    } catch (OperationException failure) {
      return page(
          409, action, bucket, key, "operationReason" + failure.reason().name(), Map.of(), ui);
    } catch (IOException failure) {
      return page(500, action, bucket, key, "operationFailed", Map.of(), ui);
    }
  }

  private Response page(
      int status,
      String action,
      String bucket,
      String key,
      String message,
      Map<String, String> errors,
      UiContext ui) {
    return Response.status(status)
        .type(MediaType.TEXT_HTML_TYPE)
        .header("Cache-Control", "no-store")
        .header("X-StackEmu-Action", "workspace")
        .entity(form(action, value(bucket), value(key), message, errors, ui).render())
        .build();
  }

  private TemplateInstance form(
      String action,
      String bucket,
      String key,
      String message,
      Map<String, String> errors,
      UiContext ui) {
    String command =
        S3CliCommands.equivalent(
            action,
            configuration.endpoint(),
            configuration.region(),
            bucket,
            "folder".equals(action) && !key.endsWith("/") ? key + "/" : key);
    return Templates.form(
        new MutationPageView(action, bucket, key, message, errors, command, maximumUpload), ui);
  }

  private static String value(String value) {
    return value == null ? "" : value;
  }

  private static String formPath(String action, String bucket, String key) {
    return "/s3/write?action="
        + S3ViewMapper.encode(action)
        + "&bucket="
        + S3ViewMapper.encode(value(bucket))
        + "&prefix="
        + S3ViewMapper.encode(value(key));
  }
}
