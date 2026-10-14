package io.github.erf88.stackemu.adapter.web;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.github.erf88.stackemu.application.*;
import io.github.erf88.stackemu.adapter.web.*;
@Path("/") @Produces(MediaType.TEXT_HTML)
public class BucketsResource {
    @Inject BucketReader reader;
    @Inject ConnectionView configuration;
    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance index(List<String> buckets, boolean connected, Ui ui);
        public static native TemplateInstance buckets(List<String> buckets, boolean connected, Ui ui);
        public static native TemplateInstance connection(List<String> buckets, boolean connected, Ui ui, ConnectionView configuration);
        public static native TemplateInstance gallery(Ui ui);
    }
    @GET
    public TemplateInstance index(@QueryParam("lang") String lang, @QueryParam("theme") String theme, @HeaderParam("Accept-Language") String accept) {
        BucketSnapshot s = reader.read();
        return Templates.index(s.buckets(), s.connected(), Ui.resolve(lang, accept, theme, "/"));
    }
    @GET @Path("buckets")
    public TemplateInstance buckets(@QueryParam("lang") String lang, @QueryParam("theme") String theme, @HeaderParam("Accept-Language") String accept) {
        BucketSnapshot s = reader.read();
        return Templates.buckets(s.buckets(), s.connected(), Ui.resolve(lang, accept, theme, "/"));
    }
    @GET @Path("connection")
    public TemplateInstance connection(@QueryParam("lang") String lang, @QueryParam("theme") String theme, @HeaderParam("Accept-Language") String accept) {
        BucketSnapshot s = reader.read();
        return Templates.connection(s.buckets(), s.connected(), Ui.resolve(lang, accept, theme, "/connection"), configuration);
    }
    @GET @Path("components")
    public TemplateInstance gallery(@QueryParam("lang") String lang, @QueryParam("theme") String theme, @HeaderParam("Accept-Language") String accept) {
        return Templates.gallery(Ui.resolve(lang, accept, theme, "/components"));
    }
}
