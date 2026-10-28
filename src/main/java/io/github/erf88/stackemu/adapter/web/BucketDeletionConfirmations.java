package io.github.erf88.stackemu.adapter.web;

import io.github.erf88.stackemu.application.S3BucketDeletionPort.Target;
import jakarta.enterprise.context.ApplicationScoped;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Separate one-use bucket capabilities; object deletion tickets are never accepted. */
@ApplicationScoped
public class BucketDeletionConfirmations {
  private static final Duration LIFETIME = Duration.ofMinutes(5);
  private static final int CAPACITY = 128;
  private final Clock clock;
  private final SecureRandom random = new SecureRandom();
  private final Map<String, Confirmation> pending = new HashMap<>();

  public BucketDeletionConfirmations() {
    this(Clock.systemUTC());
  }

  BucketDeletionConfirmations(Clock clock) {
    this.clock = clock;
  }

  private record Confirmation(Target target, Instant expires) {}

  public synchronized Optional<String> issue(Target target) {
    Instant now = clock.instant();
    pending.values().removeIf(confirmation -> !now.isBefore(confirmation.expires()));
    if (pending.size() >= CAPACITY) return Optional.empty();
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    pending.put(ticket, new Confirmation(target, now.plus(LIFETIME)));
    return Optional.of(ticket);
  }

  public synchronized Optional<Target> consume(String ticket) {
    if (ticket == null || !ticket.matches("[A-Za-z0-9_-]{43}")) return Optional.empty();
    Confirmation confirmation = pending.remove(ticket);
    return confirmation != null && clock.instant().isBefore(confirmation.expires())
        ? Optional.of(confirmation.target())
        : Optional.empty();
  }
}
