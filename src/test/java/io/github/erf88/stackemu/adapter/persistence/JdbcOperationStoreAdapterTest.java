package io.github.erf88.stackemu.adapter.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.erf88.stackemu.application.S3OperationPort.Item;
import io.github.erf88.stackemu.application.S3OperationPort.ItemState;
import io.github.erf88.stackemu.application.S3OperationPort.Kind;
import io.github.erf88.stackemu.application.S3OperationPort.Request;
import io.github.erf88.stackemu.application.S3OperationPort.State;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JdbcOperationStoreAdapterTest {
  @TempDir Path directory;

  private JdbcOperationStoreAdapter open() {
    return new JdbcOperationStoreAdapter(directory.resolve("operations").toString(), 268435456, 30);
  }

  private Request request(String bucket) {
    return new Request(Kind.EMPTY_BUCKET, bucket, "", "", "", "", false);
  }

  @Test
  void restartPreservesInventoryAndMarksInFlightItemsUncertain() {
    var first = open();
    String id = first.create(request("owned-bucket"));
    first.bindEndpoint(id, "endpoint-a");
    first.append(
        id,
        List.of(
            new Item(1, "a.txt", "", "\"etag\"", 4, Instant.now(), false, ItemState.PENDING, "")));
    first.transition(id, State.PREPARING, State.READY, "");
    assertTrue(first.claim(id));
    first.transition(id, State.READY, State.RUNNING, "");
    first.itemState(id, 1, ItemState.IN_FLIGHT, "");
    first.close();
    var second = open();
    try {
      second.recover();
      assertEquals(State.INTERRUPTED, second.get(id).state());
      assertTrue(second.matchesEndpoint(id, "endpoint-a"));
      assertFalse(second.matchesEndpoint(id, "endpoint-b"));
      assertEquals(ItemState.UNCERTAIN, second.items(id, 0, 50).getFirst().state());
      assertEquals(1, second.get(id).total());
      assertTrue(second.claim(id));
    } finally {
      second.close();
    }
  }

  @Test
  void bucketClaimAndStateTransitionAreExclusive() {
    var store = open();
    try {
      String first = store.create(request("same-bucket"));
      String second = store.create(request("same-bucket"));
      assertTrue(store.claim(first));
      assertFalse(store.claim(second));
      store.release(first);
      assertTrue(store.claim(second));
      assertTrue(store.transition(first, State.PREPARING, State.READY, ""));
      assertFalse(store.transition(first, State.PREPARING, State.READY, ""));
    } finally {
      store.close();
    }
  }

  @Test
  void finalBucketMutationIntentSurvivesRestart() {
    var store = open();
    String id = store.create(request("owned-bucket"));
    store.finish(id, State.RUNNING, "FINALIZING");
    store.close();
    var restored = open();
    try {
      restored.recover();
      assertEquals(State.INTERRUPTED, restored.get(id).state());
      assertEquals("FINALIZING", restored.get(id).message());
    } finally {
      restored.close();
    }
  }
}
