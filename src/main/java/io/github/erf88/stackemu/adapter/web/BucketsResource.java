package io.github.erf88.stackemu.adapter.web;

import io.github.erf88.stackemu.application.BucketListPort;
import io.github.erf88.stackemu.application.BucketListSnapshot;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/s3")
@Produces(MediaType.TEXT_HTML)
public class BucketsResource {
  private final BucketListPort bucketList;
  private final ConnectionSettingsView configuration;

  public BucketsResource(BucketListPort bucketList, ConnectionSettingsView configuration) {
    this.bucketList = bucketList;
    this.configuration = configuration;
  }

  @CheckedTemplate
  public static class Templates {
    public static native TemplateInstance index(
        List<String> buckets,
        boolean connected,
        UiContext ui,
        ConnectionSettingsView configuration);

    public static native TemplateInstance buckets(
        List<String> buckets,
        boolean connected,
        UiContext ui,
        ConnectionSettingsView configuration);
  }

  @GET
  public TemplateInstance showService(
      @QueryParam("lang") String lang,
      @QueryParam("theme") String theme,
      @QueryParam("density") String density,
      @HeaderParam("Accept-Language") String accept) {
    BucketListSnapshot snapshot = bucketList.listBuckets();
    return Templates.index(
        snapshot.buckets(),
        snapshot.connected(),
        UiContext.resolve(lang, accept, theme, density, "/s3"),
        configuration);
  }
}
