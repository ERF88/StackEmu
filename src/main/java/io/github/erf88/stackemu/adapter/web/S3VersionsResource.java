package io.github.erf88.stackemu.adapter.web;

import io.github.erf88.stackemu.application.OperationException;
import io.github.erf88.stackemu.application.S3ObjectToolsPort;
import io.github.erf88.stackemu.application.WriteValidationException;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/s3/versions")
@Blocking
@Produces(MediaType.TEXT_HTML)
public class S3VersionsResource {
  private final S3ObjectToolsPort tools;

  public S3VersionsResource(S3ObjectToolsPort tools) {
    this.tools = tools;
  }

  public record VersionRow(
      String key, String id, boolean marker, boolean latest, String modified, String deleteHref) {}

  @CheckedTemplate
  public static class Templates {
    public static native TemplateInstance index(
        String bucket,
        List<VersionRow> versions,
        String next,
        String back,
        String message,
        boolean deletionAvailable,
        UiContext ui);
  }

  @GET
  public Response list(
      @QueryParam("bucket") String bucket,
      @QueryParam("key") String key,
      @QueryParam("keyMarker") String keyMarker,
      @QueryParam("versionMarker") String versionMarker,
      @QueryParam("lang") String lang,
      @QueryParam("theme") String theme,
      @QueryParam("density") String density) {
    String safeBucket = bucket == null ? "" : bucket;
    String exactKey = key == null ? "" : key;
    String path =
        "/s3/versions?bucket="
            + S3ViewMapper.encode(safeBucket)
            + "&key="
            + S3ViewMapper.encode(exactKey);
    String current =
        path
            + (keyMarker == null ? "" : "&keyMarker=" + S3ViewMapper.encode(keyMarker))
            + (versionMarker == null ? "" : "&versionMarker=" + S3ViewMapper.encode(versionMarker));
    var ui = UiContext.resolve(lang, null, theme, density, current);
    String back =
        (exactKey.isEmpty()
                ? S3ViewMapper.bucketPath(safeBucket, "", null)
                : S3ViewMapper.objectPath(safeBucket, exactKey))
            + "&"
            + ui.query();
    try {
      boolean deletionAvailable = tools.isVersionDeletionAvailable(bucket);
      var result = tools.versions(bucket, exactKey, keyMarker, versionMarker);
      var rows =
          result.entries().stream()
              .map(
                  v ->
                      new VersionRow(
                          v.key(),
                          v.id(),
                          v.marker(),
                          v.latest(),
                          v.modified().toString(),
                          S3ViewMapper.deletionPath(bucket, v.key())
                              + "&version="
                              + S3ViewMapper.encode(v.id())
                              + "&"
                              + ui.query()))
              .toList();
      String next =
          result.nextKey().isEmpty()
              ? ""
              : path
                  + "&keyMarker="
                  + S3ViewMapper.encode(result.nextKey())
                  + "&versionMarker="
                  + S3ViewMapper.encode(result.nextVersion())
                  + "&"
                  + ui.query();
      return Response.ok(
              Templates.index(bucket, rows, next, back, "", deletionAvailable, ui).render())
          .header("X-StackEmu-Fragment", "object-inspector")
          .header("Cache-Control", "no-store")
          .build();
    } catch (OperationException failure) {
      return Response.status(409)
          .entity(
              Templates.index(
                      safeBucket,
                      List.of(),
                      "",
                      back,
                      "operationReason" + failure.reason().name(),
                      false,
                      ui)
                  .render())
          .build();
    } catch (IllegalArgumentException | WriteValidationException invalid) {
      return Response.status(422)
          .entity(
              Templates.index(safeBucket, List.of(), "", back, "invalidRequest", false, ui)
                  .render())
          .build();
    }
  }
}
