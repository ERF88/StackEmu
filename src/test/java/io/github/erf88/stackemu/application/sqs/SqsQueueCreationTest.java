package io.github.erf88.stackemu.application.sqs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SqsQueueCreationTest {
  @Test
  void sourceCreationFailureRetainsOnlyTheCreatedDeadLetterQueue() {
    RecordingQueues queues = new RecordingQueues();
    queues.failSourceCreation = true;

    SqsQueueCreationException failure =
        assertThrows(
            SqsQueueCreationException.class,
            () -> new SqsQueueCreation(queues).createWithDeadLetterQueue("orders", 5));

    assertEquals(SqsException.Kind.CONFLICT, failure.failure().kind());
    assertEquals("orders", failure.queueName());
    assertEquals("orders-dlq", failure.deadLetterQueueName());
    assertFalse(failure.queueCreated());
    assertEquals(List.of("orders-dlq"), queues.created);
    assertTrue(queues.updated.isEmpty());
  }

  @Test
  void associationFailureRetainsBothQueuesWithoutRetryingOrDeletingEither() {
    RecordingQueues queues = new RecordingQueues();
    queues.failAssociation = true;

    SqsQueueCreationException failure =
        assertThrows(
            SqsQueueCreationException.class,
            () -> new SqsQueueCreation(queues).createWithDeadLetterQueue("events.fifo", 7));

    assertEquals(SqsException.Kind.UNAVAILABLE, failure.failure().kind());
    assertEquals("events.fifo", failure.queueName());
    assertEquals("events-dlq.fifo", failure.deadLetterQueueName());
    assertTrue(failure.queueCreated());
    assertEquals(List.of("events-dlq.fifo", "events.fifo"), queues.created);
    assertTrue(queues.deleted.isEmpty());
    assertEquals(1, queues.updated.size());
  }

  @Test
  void successfulAssociationUsesTheRequestedMaximumReceiveCount() {
    RecordingQueues queues = new RecordingQueues();

    new SqsQueueCreation(queues).createWithDeadLetterQueue("orders", 9);

    assertEquals(List.of("orders-dlq", "orders"), queues.created);
    assertEquals(1, queues.updated.size());
    assertEquals("orders-dlq", queues.updated.getFirst().deadLetterQueueName());
    assertEquals(9, queues.updated.getFirst().maxReceiveCount());
  }

  private static final class RecordingQueues implements SqsQueuePort {
    private final List<String> created = new ArrayList<>();
    private final List<RedrivePolicy> updated = new ArrayList<>();
    private final List<String> deleted = new ArrayList<>();
    private boolean failSourceCreation;
    private boolean failAssociation;

    @Override
    public List<Queue> listQueues() {
      return List.of();
    }

    @Override
    public Queue getQueue(String name) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void createQueue(CreateQueue request) {
      if (failSourceCreation && request.name().equals("orders")) {
        throw new SqsException(SqsException.Kind.CONFLICT);
      }
      created.add(request.name());
    }

    @Override
    public void updateQueue(
        String name,
        Settings settings,
        Map<String, String> tags,
        Optional<RedrivePolicy> redrivePolicy) {
      updated.add(redrivePolicy.orElseThrow());
      if (failAssociation) {
        throw new SqsException(SqsException.Kind.UNAVAILABLE);
      }
    }

    @Override
    public void purgeQueue(String name) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void deleteQueue(String name) {
      deleted.add(name);
    }
  }
}
