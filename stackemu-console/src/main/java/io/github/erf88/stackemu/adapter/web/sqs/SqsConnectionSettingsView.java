package io.github.erf88.stackemu.adapter.web.sqs;

import jakarta.enterprise.context.ApplicationScoped;
import java.net.URI;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class SqsConnectionSettingsView {
  private final URI endpoint;
  private final String region;

  public SqsConnectionSettingsView(
      @ConfigProperty(name = "stackemu.public-endpoint", defaultValue = "http://127.0.0.1:4566")
          URI endpoint,
      @ConfigProperty(name = "quarkus.sqs.aws.region") String region) {
    this.endpoint = endpoint;
    this.region = region;
  }

  public String endpoint() {
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
