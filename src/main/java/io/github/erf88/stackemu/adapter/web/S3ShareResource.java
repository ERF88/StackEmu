package io.github.erf88.stackemu.adapter.web;

import io.github.erf88.stackemu.application.OperationException;
import io.github.erf88.stackemu.application.S3ObjectToolsPort;
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

@Path("/s3/share")
@Blocking
@Produces(MediaType.TEXT_HTML)
public class S3ShareResource {
  private final S3ObjectToolsPort tools;

  public S3ShareResource(S3ObjectToolsPort tools) {
    this.tools = tools;
  }

  @CheckedTemplate
  public static class Templates {
    public static native TemplateInstance form(
        String bucket, String key, String url, String expires, String message, UiContext ui);
  }

  @GET
  public Response form(
      @QueryParam("bucket") String bucket,
      @QueryParam("key") String key,
      @QueryParam("lang") String lang,
      @QueryParam("theme") String theme,
      @QueryParam("density") String density) {
    return page(
        bucket,
        key,
        "",
        "",
        "",
        UiContext.resolve(lang, null, theme, density, sharePath(bucket, key)),
        200);
  }

  @POST
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public Response sign(
      @RestForm String bucket,
      @RestForm String key,
      @RestForm int minutes,
      @RestForm String lang,
      @RestForm String theme,
      @RestForm String density) {
    var ui = UiContext.resolve(lang, null, theme, density, sharePath(bucket, key));
    try {
      var result =
          tools.signDownload(bucket, key, minutes, DownloadHeaders.attachmentDisposition(key));
      return page(bucket, key, result.url(), result.expires().toString(), "", ui, 200);
    } catch (OperationException failure) {
      return page(bucket, key, "", "", "operationReason" + failure.reason().name(), ui, 409);
    } catch (IllegalArgumentException | WriteValidationException invalid) {
      return page(bucket, key, "", "", "invalidRequest", ui, 422);
    }
  }

  private static String sharePath(String bucket, String key) {
    return "/s3/share?bucket="
        + S3ViewMapper.encode(bucket == null ? "" : bucket)
        + "&key="
        + S3ViewMapper.encode(key == null ? "" : key);
  }

  private static Response page(
      String bucket,
      String key,
      String url,
      String expires,
      String message,
      UiContext ui,
      int status) {
    return Response.status(status)
        .header("Cache-Control", "no-store")
        .header("Referrer-Policy", "no-referrer")
        .header("X-StackEmu-Action", "workspace")
        .entity(
            Templates.form(
                    bucket == null ? "" : bucket, key == null ? "" : key, url, expires, message, ui)
                .render())
        .build();
  }
}
