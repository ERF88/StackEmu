package io.github.erf88.stackemu.application;

import io.github.erf88.stackemu.application.S3OperationPort.Item;
import io.github.erf88.stackemu.application.S3OperationPort.ItemState;
import io.github.erf88.stackemu.application.S3OperationPort.Operation;
import io.github.erf88.stackemu.application.S3OperationPort.Request;
import io.github.erf88.stackemu.application.S3OperationPort.State;
import java.util.List;

/** Atomic local state changes; no remote I/O or localized messages. */
public interface OperationStorePort {
  String create(Request request);

  Operation get(String id);

  List<Operation> list(int page);

  List<Item> items(String id, long after, int limit);

  void append(String id, List<Item> items);

  void metadata(String id, String versioning, String destinationEtag);

  boolean transition(String id, State expected, State next, String message);

  void finish(String id, State state, String message);

  void itemState(String id, long sequence, ItemState state, String message);

  boolean claim(String id);

  void release(String id);

  void recover();

  void bindEndpoint(String id, String endpoint);

  boolean matchesEndpoint(String id, String endpoint);
}
