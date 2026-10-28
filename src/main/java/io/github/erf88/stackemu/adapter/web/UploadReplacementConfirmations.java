package io.github.erf88.stackemu.adapter.web;

import jakarta.enterprise.context.ApplicationScoped;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Independent upload replacement review; never trusts an ETag supplied by the browser. */
@ApplicationScoped
public class UploadReplacementConfirmations {
  public record Target(String bucket, String key, String etag, Instant expires) {}

  private final Map<String, Target> pending = new HashMap<>();
  private final SecureRandom random = new SecureRandom();

  public synchronized String issue(String bucket, String key, String etag) {
    pending.values().removeIf(target -> target.expires().isBefore(Instant.now()));
    if (pending.size() >= 128) return "";
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    pending.put(token, new Target(bucket, key, etag, Instant.now().plusSeconds(1800)));
    return token;
  }

  public synchronized Optional<Target> consume(String token) {
    Target target = pending.remove(token);
    return target != null && target.expires().isAfter(Instant.now())
        ? Optional.of(target)
        : Optional.empty();
  }
}
