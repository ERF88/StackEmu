package io.github.erf88.stackemu.adapter.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.erf88.stackemu.application.S3BucketCleanupPort.Manifest;
import io.github.erf88.stackemu.application.S3BucketDeletionPort.Target;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class BucketCleanupConfirmationsTest {
  @Test
  void ticketsAreIndependentBoundedOpaqueAndSingleUse() {
    var store = new BucketCleanupConfirmations();
    var manifest = new Manifest("test-bucket", List.of());
    var tickets = new HashSet<String>();
    for (int index = 0; index < 32; index++) {
      String ticket = store.issue(manifest).orElseThrow();
      assertTrue(ticket.matches("[A-Za-z0-9_-]{43}"));
      assertTrue(tickets.add(ticket));
    }
    assertTrue(store.issue(manifest).isEmpty());
    String ticket = tickets.iterator().next();
    assertEquals(manifest, store.consume(ticket).orElseThrow());
    assertTrue(store.consume(ticket).isEmpty());
    assertTrue(store.consume(null).isEmpty());
    assertTrue(store.consume("tampered").isEmpty());
    assertTrue(store.issue(manifest).isPresent());
    var empty = new BucketDeletionConfirmations();
    String foreign = empty.issue(new Target("test-bucket")).orElseThrow();
    assertTrue(store.consume(foreign).isEmpty());
    assertTrue(empty.consume(tickets.iterator().next()).isEmpty());
    assertTrue(empty.consume(foreign).isPresent());
  }

  @Test
  void exactExpiryReleasesManifestCapacity() {
    var now = new AtomicReference<>(Instant.EPOCH);
    Clock clock =
        new Clock() {
          public ZoneId getZone() {
            return ZoneOffset.UTC;
          }

          public Clock withZone(ZoneId zone) {
            return this;
          }

          public Instant instant() {
            return now.get();
          }
        };
    var store = new BucketCleanupConfirmations(clock);
    var manifest = new Manifest("test-bucket", List.of());
    String ticket = store.issue(manifest).orElseThrow();
    for (int index = 1; index < 32; index++) store.issue(manifest).orElseThrow();
    now.set(Instant.EPOCH.plusSeconds(300));
    assertTrue(store.consume(ticket).isEmpty());
    for (int index = 0; index < 32; index++) assertTrue(store.issue(manifest).isPresent());
    assertTrue(store.issue(manifest).isEmpty());
  }
}
