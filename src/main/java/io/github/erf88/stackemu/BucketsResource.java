package io.github.erf88.stackemu;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;

@Path("/")
@Produces(MediaType.TEXT_HTML)
public class BucketsResource {
    @Inject S3Client s3;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance index(List<String> buckets, boolean connected, boolean portuguese);
        public static native TemplateInstance buckets(List<String> buckets, boolean connected, boolean portuguese);
    }

    @GET
    public TemplateInstance index(@QueryParam("lang") String lang) {
        return render(false, "pt-BR".equals(lang));
    }

    @GET
    @Path("buckets")
    public TemplateInstance buckets(@QueryParam("lang") String lang) {
        return render(true, "pt-BR".equals(lang));
    }

    private TemplateInstance render(boolean fragment, boolean portuguese) {
        List<String> names = List.of();
        boolean connected = false;
        try {
            names = s3.listBuckets().buckets().stream().map(b -> b.name()).sorted().toList();
            connected = true;
        } catch (SdkException ignored) {
            // Endpoint failures are normal UI states. Never expose SDK messages or credentials.
        }
        return fragment ? Templates.buckets(names, connected, portuguese)
                : Templates.index(names, connected, portuguese);
    }
}
