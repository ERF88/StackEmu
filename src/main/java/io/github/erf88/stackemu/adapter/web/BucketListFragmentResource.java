package io.github.erf88.stackemu.adapter.web;

import io.github.erf88.stackemu.application.BucketListPort;
import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/** Preserves the existing read-only fragment URL independently of the S3 page root. */
@Path("/buckets")
@Produces(MediaType.TEXT_HTML)
public class BucketListFragmentResource {
  private final BucketListPort bucketList;
  private final ConnectionSettingsView configuration;

  public BucketListFragmentResource(
      BucketListPort bucketList, ConnectionSettingsView configuration) {
    this.bucketList = bucketList;
    this.configuration = configuration;
  }

  @GET
  public TemplateInstance showBuckets(
      @QueryParam("lang") String language,
      @QueryParam("theme") String theme,
      @QueryParam("density") String density,
      @HeaderParam("Accept-Language") String acceptLanguage) {
    var snapshot = bucketList.listBuckets();
    return BucketsResource.Templates.buckets(
        snapshot.buckets(),
        snapshot.connected(),
        UiContext.resolve(language, acceptLanguage, theme, density, "/s3"),
        configuration);
  }
}
