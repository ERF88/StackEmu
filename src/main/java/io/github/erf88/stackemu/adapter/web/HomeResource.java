package io.github.erf88.stackemu.adapter.web;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/** Service entry point. Rendering the home never probes an emulator. */
@Path("/")
@Produces(MediaType.TEXT_HTML)
public class HomeResource {
  @CheckedTemplate
  public static class Templates {
    public static native TemplateInstance index(UiContext ui);
  }

  @GET
  public TemplateInstance showHome(
      @QueryParam("lang") String language,
      @QueryParam("theme") String theme,
      @QueryParam("density") String density,
      @HeaderParam("Accept-Language") String acceptLanguage) {
    return Templates.index(UiContext.resolve(language, acceptLanguage, theme, density, "/"));
  }
}
