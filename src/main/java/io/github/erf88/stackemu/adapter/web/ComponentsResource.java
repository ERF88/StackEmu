package io.github.erf88.stackemu.adapter.web;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/components")
@Produces(MediaType.TEXT_HTML)
public class ComponentsResource {
  @CheckedTemplate
  public static class Templates {
    public static native TemplateInstance gallery(UiContext ui);
  }

  @GET
  public TemplateInstance showGallery(
      @QueryParam("lang") String language,
      @QueryParam("theme") String theme,
      @HeaderParam("Accept-Language") String acceptLanguage) {
    return Templates.gallery(UiContext.resolve(language, acceptLanguage, theme, "/components"));
  }
}
