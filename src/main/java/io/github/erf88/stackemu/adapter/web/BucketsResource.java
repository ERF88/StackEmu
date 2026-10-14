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

@Path("/")
@Produces(MediaType.TEXT_HTML)
public class BucketsResource {
  private final BucketListPort bucketList;

  public BucketsResource(BucketListPort bucketList) {
    this.bucketList = bucketList;
  }

  @CheckedTemplate
  public static class Templates {
    public static native TemplateInstance index(
        List<String> buckets, boolean connected, UiContext ui);

    public static native TemplateInstance buckets(
        List<String> buckets, boolean connected, UiContext ui);
  }

  @GET
  public TemplateInstance showDashboard(
      @QueryParam("lang") String lang,
      @QueryParam("theme") String theme,
      @HeaderParam("Accept-Language") String accept) {
    BucketListSnapshot snapshot = bucketList.listBuckets();
    return Templates.index(
        snapshot.buckets(), snapshot.connected(), UiContext.resolve(lang, accept, theme, "/"));
  }

  @GET
  @Path("buckets")
  public TemplateInstance showBuckets(
      @QueryParam("lang") String lang,
      @QueryParam("theme") String theme,
      @HeaderParam("Accept-Language") String accept) {
    BucketListSnapshot snapshot = bucketList.listBuckets();
    return Templates.buckets(
        snapshot.buckets(), snapshot.connected(), UiContext.resolve(lang, accept, theme, "/"));
  }
}
