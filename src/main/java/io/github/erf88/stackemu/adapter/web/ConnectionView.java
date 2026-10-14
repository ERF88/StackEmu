package io.github.erf88.stackemu.adapter.web;

import jakarta.enterprise.context.ApplicationScoped;
import java.net.URI;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ConnectionView {
  @ConfigProperty(name = "quarkus.s3.endpoint-override")
  URI endpoint;

  @ConfigProperty(name = "quarkus.s3.aws.region")
  String region;

  public String endpoint() {
    // Hide credentials, query tokens and fragments embedded in the configured URL.
    try {
      return new URI(
              endpoint.getScheme(),
              null,
              endpoint.getHost(),
              endpoint.getPort(),
              endpoint.getPath(),
              null,
              null)
          .toString();
    } catch (java.net.URISyntaxException ignored) {
      return "[redacted]";
    }
  }

  public String region() {
    return region;
  }
}
