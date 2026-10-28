package io.github.erf88.stackemu.adapter.persistence;

import io.github.erf88.stackemu.application.OperationException;
import io.github.erf88.stackemu.application.OperationStorePort;
import io.github.erf88.stackemu.application.S3OperationPort.Item;
import io.github.erf88.stackemu.application.S3OperationPort.ItemState;
import io.github.erf88.stackemu.application.S3OperationPort.Kind;
import io.github.erf88.stackemu.application.S3OperationPort.Operation;
import io.github.erf88.stackemu.application.S3OperationPort.Request;
import io.github.erf88.stackemu.application.S3OperationPort.State;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.h2.jdbcx.JdbcDataSource;

/** Single-instance file store. Commit each item intent before sending its remote mutation. */
@ApplicationScoped
public class JdbcOperationStoreAdapter implements OperationStorePort {
  private final String url;
  private final Path file;
  private final long maximumBytes;
  private final int retentionDays;
  private boolean initialized;
  private Connection keepAlive;
  private final JdbcDataSource dataSource;

  public JdbcOperationStoreAdapter(
      @ConfigProperty(name = "stackemu.operations.path", defaultValue = ".stackemu/operations")
          String path,
      @ConfigProperty(name = "stackemu.operations.max-storage", defaultValue = "268435456")
          long maximumBytes,
      @ConfigProperty(name = "stackemu.operations.retention-days", defaultValue = "30")
          int retentionDays) {
    this.file = Path.of(path).toAbsolutePath().normalize();
    this.url =
        "jdbc:h2:file:"
            + file.toString().replace('\\', '/')
            + ";DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0";
    this.dataSource = new JdbcDataSource();
    this.dataSource.setURL(url);
    if (maximumBytes < 1048576 || retentionDays < 1)
      throw new IllegalArgumentException("operation bounds");
    this.maximumBytes = maximumBytes;
    this.retentionDays = retentionDays;
  }

  private synchronized Connection open() throws SQLException {
    if (!initialized) {
      try {
        Files.createDirectories(file.getParent());
      } catch (java.io.IOException failure) {
        throw new SQLException("storage", failure);
      }
      keepAlive = dataSource.getConnection();
      try (var sql = keepAlive.createStatement()) {
        sql.execute("CREATE TABLE IF NOT EXISTS schema_version (version INT PRIMARY KEY)");
        sql.execute(
            "CREATE TABLE IF NOT EXISTS operations (id VARCHAR(36) PRIMARY KEY, kind VARCHAR(30) NOT NULL, bucket VARCHAR(63) NOT NULL, object_key VARCHAR(2048) NOT NULL, version_id VARCHAR(1024) NOT NULL, destination_bucket VARCHAR(63) NOT NULL, destination_key VARCHAR(2048) NOT NULL, replace_object BOOLEAN NOT NULL, state VARCHAR(30) NOT NULL, created BIGINT NOT NULL, updated BIGINT NOT NULL, total BIGINT DEFAULT 0, done BIGINT DEFAULT 0, message VARCHAR(100) DEFAULT '', versioning VARCHAR(30) DEFAULT '', destination_etag VARCHAR(256) DEFAULT '')");
        sql.execute(
            "CREATE TABLE IF NOT EXISTS operation_items (operation_id VARCHAR(36) NOT NULL REFERENCES operations(id) ON DELETE CASCADE, sequence BIGINT NOT NULL, object_key VARCHAR(2048) NOT NULL, version_id VARCHAR(1024) NOT NULL, etag VARCHAR(256) NOT NULL, object_size BIGINT NOT NULL, modified VARCHAR(40) NOT NULL, marker BOOLEAN NOT NULL, state VARCHAR(30) NOT NULL, message VARCHAR(100) NOT NULL, PRIMARY KEY(operation_id, sequence), UNIQUE(operation_id, object_key, version_id))");
        sql.execute(
            "CREATE TABLE IF NOT EXISTS operation_locks (bucket VARCHAR(63) PRIMARY KEY, operation_id VARCHAR(36) NOT NULL)");
        sql.execute(
            "ALTER TABLE operations ADD COLUMN IF NOT EXISTS endpoint_id VARCHAR(64) DEFAULT ''");
        sql.execute("MERGE INTO schema_version KEY(version) VALUES (2)");
      }
      initialized = true;
    }
    return dataSource.getConnection();
  }

  private void capacity() {
    try {
      if (Files.exists(Path.of(file + ".mv.db"))
          && Files.size(Path.of(file + ".mv.db")) >= maximumBytes)
        throw new OperationException(OperationException.Reason.CAPACITY);
    } catch (java.io.IOException failure) {
      throw unavailable(failure);
    }
  }

  private static OperationException unavailable(Throwable cause) {
    return new OperationException(OperationException.Reason.UNAVAILABLE, cause);
  }

  @Override
  public synchronized String create(Request request) {
    capacity();
    String id = UUID.randomUUID().toString();
    try (var connection = open();
        var cleanup =
            connection.prepareStatement(
                "DELETE FROM operations WHERE updated < ? AND state IN ('SUCCEEDED','FAILED','PARTIAL')");
        var sql =
            connection.prepareStatement(
                "INSERT INTO operations (id,kind,bucket,object_key,version_id,destination_bucket,destination_key,replace_object,state,created,updated) VALUES (?,?,?,?,?,?,?,?,?,?,?)")) {
      cleanup.setLong(1, Instant.now().minus(retentionDays, ChronoUnit.DAYS).toEpochMilli());
      cleanup.executeUpdate();
      sql.setString(1, id);
      sql.setString(2, request.kind().name());
      sql.setString(3, request.bucket());
      sql.setString(4, request.key());
      sql.setString(5, request.version());
      sql.setString(6, request.destinationBucket());
      sql.setString(7, request.destinationKey());
      sql.setBoolean(8, request.replace());
      sql.setString(9, State.PREPARING.name());
      sql.setLong(10, System.currentTimeMillis());
      sql.setLong(11, System.currentTimeMillis());
      sql.executeUpdate();
      return id;
    } catch (SQLException failure) {
      throw unavailable(failure);
    }
  }

  @Override
  public synchronized Operation get(String id) {
    try (var connection = open();
        var sql = connection.prepareStatement("SELECT * FROM operations WHERE id=?")) {
      sql.setString(1, id);
      try (var rows = sql.executeQuery()) {
        if (!rows.next()) throw new OperationException(OperationException.Reason.MISSING);
        return operation(rows);
      }
    } catch (SQLException failure) {
      throw unavailable(failure);
    }
  }

  @Override
  public synchronized List<Operation> list(int page) {
    try (var connection = open();
        var sql =
            connection.prepareStatement(
                "SELECT * FROM operations ORDER BY created DESC,id LIMIT 50 OFFSET ?")) {
      sql.setLong(1, Math.max(0L, Math.min(page, 100000)) * 50);
      try (var rows = sql.executeQuery()) {
        List<Operation> result = new ArrayList<>();
        while (rows.next()) result.add(operation(rows));
        return List.copyOf(result);
      }
    } catch (SQLException failure) {
      throw unavailable(failure);
    }
  }

  private static Operation operation(ResultSet row) throws SQLException {
    return new Operation(
        row.getString("id"),
        new Request(
            Kind.valueOf(row.getString("kind")),
            row.getString("bucket"),
            row.getString("object_key"),
            row.getString("version_id"),
            row.getString("destination_bucket"),
            row.getString("destination_key"),
            row.getBoolean("replace_object")),
        State.valueOf(row.getString("state")),
        Instant.ofEpochMilli(row.getLong("created")),
        Instant.ofEpochMilli(row.getLong("updated")),
        row.getLong("total"),
        row.getLong("done"),
        row.getString("message"),
        row.getString("versioning"),
        row.getString("destination_etag"));
  }

  @Override
  public synchronized List<Item> items(String id, long after, int limit) {
    try (var connection = open();
        var sql =
            connection.prepareStatement(
                "SELECT * FROM operation_items WHERE operation_id=? AND sequence>? ORDER BY sequence LIMIT ?")) {
      sql.setString(1, id);
      sql.setLong(2, after);
      sql.setInt(3, Math.clamp(limit, 1, 1000));
      try (var rows = sql.executeQuery()) {
        List<Item> result = new ArrayList<>();
        while (rows.next())
          result.add(
              new Item(
                  rows.getLong("sequence"),
                  rows.getString("object_key"),
                  rows.getString("version_id"),
                  rows.getString("etag"),
                  rows.getLong("object_size"),
                  Instant.parse(rows.getString("modified")),
                  rows.getBoolean("marker"),
                  ItemState.valueOf(rows.getString("state")),
                  rows.getString("message")));
        return List.copyOf(result);
      }
    } catch (SQLException failure) {
      throw unavailable(failure);
    }
  }

  @Override
  public synchronized void append(String id, List<Item> items) {
    capacity();
    try (var connection = open();
        var sql =
            connection.prepareStatement(
                "INSERT INTO operation_items VALUES (?,?,?,?,?,?,?,?,?,?)");
        var count =
            connection.prepareStatement(
                "UPDATE operations SET total=total+?,updated=? WHERE id=? AND state='PREPARING'")) {
      connection.setAutoCommit(false);
      for (var item : items) {
        sql.setString(1, id);
        sql.setLong(2, item.sequence());
        sql.setString(3, item.key());
        sql.setString(4, item.version());
        sql.setString(5, item.etag());
        sql.setLong(6, item.size());
        sql.setString(7, item.modified().toString());
        sql.setBoolean(8, item.marker());
        sql.setString(9, item.state().name());
        sql.setString(10, item.message());
        sql.addBatch();
      }
      sql.executeBatch();
      count.setInt(1, items.size());
      count.setLong(2, System.currentTimeMillis());
      count.setString(3, id);
      if (count.executeUpdate() != 1) throw new SQLException("state");
      connection.commit();
    } catch (SQLException failure) {
      throw unavailable(failure);
    }
  }

  @Override
  public synchronized void metadata(String id, String versioning, String etag) {
    update(
        "UPDATE operations SET versioning=?,destination_etag=? WHERE id=?", versioning, etag, id);
  }

  @Override
  public synchronized boolean transition(String id, State expected, State next, String message) {
    return update(
            "UPDATE operations SET state=?,message=?,updated=? WHERE id=? AND state=?",
            next.name(),
            message,
            System.currentTimeMillis(),
            id,
            expected.name())
        == 1;
  }

  @Override
  public synchronized void finish(String id, State state, String message) {
    update(
        "UPDATE operations SET state=?,message=?,updated=? WHERE id=?",
        state.name(),
        message,
        System.currentTimeMillis(),
        id);
  }

  @Override
  public synchronized void itemState(String id, long sequence, ItemState state, String message) {
    try (var connection = open();
        var sql =
            connection.prepareStatement(
                "UPDATE operation_items SET state=?,message=? WHERE operation_id=? AND sequence=?");
        var count =
            connection.prepareStatement(
                "UPDATE operations SET done=(SELECT COUNT(*) FROM operation_items WHERE operation_id=? AND state='DONE'),updated=? WHERE id=?")) {
      connection.setAutoCommit(false);
      sql.setString(1, state.name());
      sql.setString(2, message);
      sql.setString(3, id);
      sql.setLong(4, sequence);
      sql.executeUpdate();
      count.setString(1, id);
      count.setLong(2, System.currentTimeMillis());
      count.setString(3, id);
      count.executeUpdate();
      connection.commit();
    } catch (SQLException failure) {
      throw unavailable(failure);
    }
  }

  @Override
  public synchronized boolean claim(String id) {
    var operation = get(id);
    var buckets = new java.util.TreeSet<String>();
    buckets.add(operation.request().bucket());
    if (!operation.request().destinationBucket().isEmpty())
      buckets.add(operation.request().destinationBucket());
    try (var connection = open();
        var sql = connection.prepareStatement("INSERT INTO operation_locks VALUES (?,?)")) {
      connection.setAutoCommit(false);
      for (String bucket : buckets) {
        sql.setString(1, bucket);
        sql.setString(2, id);
        sql.executeUpdate();
      }
      connection.commit();
      return true;
    } catch (SQLException failure) {
      if ("23505".equals(failure.getSQLState())) return false;
      throw unavailable(failure);
    }
  }

  @Override
  public synchronized void release(String id) {
    update("DELETE FROM operation_locks WHERE operation_id=?", id);
  }

  @Override
  public synchronized void recover() {
    update(
        "UPDATE operation_items SET state='UNCERTAIN',message='UNCERTAIN' WHERE state='IN_FLIGHT'");
    update(
        "UPDATE operations SET state='INTERRUPTED',message=CASE WHEN message='FINALIZING' THEN message ELSE 'RESTARTED' END WHERE state IN ('RUNNING','STOP_REQUESTED')");
    update("UPDATE operations SET state='FAILED',message='RESTARTED' WHERE state='PREPARING'");
    update("DELETE FROM operation_locks");
  }

  @Override
  public synchronized void bindEndpoint(String id, String endpoint) {
    update("UPDATE operations SET endpoint_id=? WHERE id=?", endpoint, id);
  }

  @Override
  public synchronized boolean matchesEndpoint(String id, String endpoint) {
    try (var connection = open();
        var sql = connection.prepareStatement("SELECT endpoint_id FROM operations WHERE id=?")) {
      sql.setString(1, id);
      try (var rows = sql.executeQuery()) {
        return rows.next() && endpoint.equals(rows.getString(1));
      }
    } catch (SQLException failure) {
      throw unavailable(failure);
    }
  }

  private int update(String query, Object... parameters) {
    try (var connection = open();
        PreparedStatement sql = connection.prepareStatement(query)) {
      for (int i = 0; i < parameters.length; i++) sql.setObject(i + 1, parameters[i]);
      return sql.executeUpdate();
    } catch (SQLException failure) {
      throw unavailable(failure);
    }
  }

  @PreDestroy
  public synchronized void close() {
    if (keepAlive != null)
      try {
        keepAlive.close();
        initialized = false;
      } catch (SQLException failure) {
        throw unavailable(failure);
      }
  }
}
