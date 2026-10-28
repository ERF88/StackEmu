package io.github.erf88.stackemu.adapter.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.erf88.stackemu.application.S3BucketDeletionPort.Target;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class BucketDeletionConfirmationsTest {
  @Test
  void expiryBoundaryInvalidatesAndReleasesCapacity() {
    var now = new java.util.concurrent.atomic.AtomicReference<>(Instant.EPOCH);
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
    var store = new BucketDeletionConfirmations(clock);
    var target = new Target("test-bucket");
    String ticket = store.issue(target).orElseThrow();
    for (int i = 1; i < 128; i++) store.issue(target).orElseThrow();
    now.set(Instant.EPOCH.plusSeconds(300));
    assertTrue(store.consume(ticket).isEmpty());
    for (int i = 0; i < 128; i++) assertTrue(store.issue(target).isPresent());
    assertTrue(store.issue(target).isEmpty());
  }

  @Test
  void ticketsAreBoundedOpaqueExactTargetAndSingleUse() {
    var store = new BucketDeletionConfirmations();
    var target = new Target("test-bucket");
    var tickets = new HashSet<String>();
    for (int i = 0; i < 128; i++) {
      String ticket = store.issue(target).orElseThrow();
      assertTrue(ticket.matches("[A-Za-z0-9_-]{43}"));
      assertTrue(tickets.add(ticket));
    }
    assertTrue(store.issue(target).isEmpty());
    String ticket = tickets.iterator().next();
    assertEquals(target, store.consume(ticket).orElseThrow());
    assertTrue(store.consume(ticket).isEmpty());
    assertTrue(store.consume(null).isEmpty());
    assertTrue(store.consume("tampered").isEmpty());
    assertTrue(store.issue(target).isPresent());
    assertTrue(new BucketDeletionConfirmations().consume(tickets.iterator().next()).isEmpty());
  }

  @Test
  void objectTicketsCannotAuthorizeBucketDeletion() {
    var objects = new ObjectDeletionConfirmations();
    var target =
        new io.github.erf88.stackemu.application.S3ObjectDeletionPort.Target(
            "test-bucket", "file.txt", "\"abc\"", 1, Instant.EPOCH);
    String ticket = objects.issue(target).orElseThrow();
    assertTrue(new BucketDeletionConfirmations().consume(ticket).isEmpty());
    assertEquals(target, objects.consume(ticket).orElseThrow());
  }
}
