package io.github.erf88.stackemu.adapter.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.erf88.stackemu.application.S3ObjectDeletionPort.Target;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class ObjectDeletionConfirmationsTest {
  private static final Target TARGET =
      new Target("test-bucket", "a +%/ação.txt", "\"abc\"", 1, Instant.EPOCH);

  private static class MutableClock extends Clock {
    private Instant now = Instant.EPOCH;

    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    public Clock withZone(ZoneId zone) {
      return this;
    }

    public Instant instant() {
      return now;
    }
  }

  @Test
  void ticketsAreOpaqueBoundedAndUsableOnce() {
    var confirmations = new ObjectDeletionConfirmations();
    var tickets = new HashSet<String>();
    for (int index = 0; index < 128; index++) {
      String ticket = confirmations.issue(TARGET).orElseThrow();
      assertTrue(ticket.matches("[A-Za-z0-9_-]{43}"));
      assertTrue(tickets.add(ticket));
    }
    assertTrue(confirmations.issue(TARGET).isEmpty());
    String ticket = tickets.iterator().next();
    assertEquals(TARGET, confirmations.consume(ticket).orElseThrow());
    assertTrue(confirmations.consume(ticket).isEmpty());
    assertTrue(confirmations.issue(TARGET).isPresent());
    assertTrue(confirmations.consume(null).isEmpty());
    assertTrue(confirmations.consume("tampered").isEmpty());
  }

  @Test
  void expiryIsExactAndRestartInvalidatesTickets() {
    var clock = new MutableClock();
    var confirmations = new ObjectDeletionConfirmations(clock);
    String ticket = confirmations.issue(TARGET).orElseThrow();
    assertTrue(new ObjectDeletionConfirmations(clock).consume(ticket).isEmpty());
    clock.now = clock.now.plusSeconds(300);
    assertTrue(confirmations.consume(ticket).isEmpty());
    assertTrue(confirmations.issue(TARGET).isPresent());
  }
}
