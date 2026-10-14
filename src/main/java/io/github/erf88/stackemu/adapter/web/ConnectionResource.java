package io.github.erf88.stackemu.adapter.web;

import io.github.erf88.stackemu.application.BucketListPort;
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
        ConnectionSettingsView configuration);
  }

  @GET
  public TemplateInstance showConnection(
      @QueryParam("lang") String language,
      @QueryParam("theme") String theme,
      @HeaderParam("Accept-Language") String acceptLanguage) {
    var snapshot = bucketList.listBuckets();
    return Templates.connection(
        snapshot.buckets(),
        snapshot.connected(),
        UiContext.resolve(language, acceptLanguage, theme, "/connection"),
        configuration);
  }
}
