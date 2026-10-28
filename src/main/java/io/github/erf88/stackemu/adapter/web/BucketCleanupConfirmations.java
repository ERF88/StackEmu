package io.github.erf88.stackemu.adapter.web;

import io.github.erf88.stackemu.application.S3BucketCleanupPort.Manifest;
import jakarta.enterprise.context.ApplicationScoped;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Independent one-use capabilities; never accept object/empty-bucket tickets. */
@ApplicationScoped
public class BucketCleanupConfirmations {
  private final Clock clock;
  private final SecureRandom random = new SecureRandom();
  private final Map<String, Confirmation> pending = new HashMap<>();

  private record Confirmation(Manifest manifest, Instant expires) {}

  public BucketCleanupConfirmations() {
    this(Clock.systemUTC());
  }

  BucketCleanupConfirmations(Clock clock) {
    this.clock = clock;
  }

  public synchronized Optional<String> issue(Manifest manifest) {
    Instant now = clock.instant();
    pending.values().removeIf(value -> !now.isBefore(value.expires()));
    if (pending.size() >= 32) return Optional.empty();
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    pending.put(ticket, new Confirmation(manifest, now.plus(Duration.ofMinutes(5))));
    return Optional.of(ticket);
  }

  public synchronized Optional<Manifest> consume(String ticket) {
    if (ticket == null || !ticket.matches("[A-Za-z0-9_-]{43}")) return Optional.empty();
    var value = pending.remove(ticket);
    return value != null && clock.instant().isBefore(value.expires())
        ? Optional.of(value.manifest())
        : Optional.empty();
  }
}
