package io.github.erf88.stackemu.adapter.web.s3;

import io.github.erf88.stackemu.adapter.web.UiContext;
import io.github.erf88.stackemu.application.s3.BucketListPort;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/connection")
@Produces(MediaType.TEXT_HTML)
public class ConnectionResource {
  private final BucketListPort bucketList;
  private final ConnectionSettingsView configuration;

  public ConnectionResource(BucketListPort bucketList, ConnectionSettingsView configuration) {
    this.bucketList = bucketList;
    this.configuration = configuration;
  }

  @CheckedTemplate
  public static class Templates {
    public static native TemplateInstance connection(
        List<String> buckets,
        boolean connected,
        UiContext ui,
        ConnectionSettingsView configuration,
        ConnectionNavigation navigation);
  }

  public record ConnectionNavigation(String href, String label) {}

  @GET
  public TemplateInstance showConnection(
      @QueryParam("lang") String language,
      @QueryParam("theme") String theme,
      @QueryParam("density") String density,
      @QueryParam("from") String from,
      @HeaderParam("Accept-Language") String acceptLanguage) {
    var snapshot = bucketList.listBuckets();
    var ui = UiContext.resolve(language, acceptLanguage, theme, density, connectionPage(from));
    return Templates.connection(
        snapshot.buckets(), snapshot.connected(), ui, configuration, navigation(from, ui));
  }

  private static ConnectionNavigation navigation(String from, UiContext ui) {
    if ("s3".equals(from)) {
      return new ConnectionNavigation("/s3?" + ui.query(), ui.text("backToS3"));
    }
    return new ConnectionNavigation("/?" + ui.query(), ui.text("backToHome"));
  }

  private static String connectionPage(String from) {
    return "s3".equals(from) ? "/connection?from=s3" : "/connection";
  }
}
