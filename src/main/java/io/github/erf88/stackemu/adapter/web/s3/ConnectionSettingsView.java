package io.github.erf88.stackemu.adapter.web.s3;

import jakarta.enterprise.context.ApplicationScoped;
import java.net.URI;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ConnectionSettingsView {
  private final URI endpoint;
  private final String region;

  public ConnectionSettingsView(
      @ConfigProperty(name = "stackemu.public-endpoint", defaultValue = "http://127.0.0.1:4566")
          URI endpoint,
      @ConfigProperty(name = "quarkus.s3.aws.region") String region) {
    this.endpoint = endpoint;
    this.region = region;
  }

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
