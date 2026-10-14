package io.github.erf88.stackemu.application;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Read-only S3 boundary; optional metadata may be absent. */
public interface S3ReadPort {
  /**
   * Reads a required bucket; optional region/tags may be unknown. Throws S3ReadException on
   * failure.
   */
  BucketDetails getBucketDetails(String bucket);

  /** Reads one remote page, never the complete listing. Throws S3ReadException on failure. */
  ObjectPage listObjects(String bucket, String prefix, String continuationToken, int pageSize);

  /** Reads remote metadata; missing size is -1. Throws S3ReadException on failure. */
  ObjectDetails getObjectDetails(String bucket, String key);

  /**
   * Opens a remote stream owned by the caller. The caller must close it even after cancellation.
   */
  Download openDownload(String bucket, String key);

  enum TagsState {
    AVAILABLE,
    EMPTY,
    UNKNOWN
  }

  record BucketDetails(
      String name, String reportedRegion, Map<String, String> tags, TagsState tagsState) {
    public BucketDetails {
      tags = Map.copyOf(tags);
    }
  }

  record ObjectSummary(String key, long size, Instant modified) {}

  record ObjectPage(List<String> prefixes, List<ObjectSummary> objects, String nextToken) {
    public ObjectPage {
      prefixes = List.copyOf(prefixes);
      objects = List.copyOf(objects);
    }
  }

  record ObjectDetails(String key, long size, String contentType, String etag, Instant modified) {}

  /** The caller owns and must close this stream, including on client disconnect. */
  record Download(InputStream body, long size, String contentType) implements AutoCloseable {
    public Download(InputStream body, long size) {
      this(body, size, null);
    }

    @Override
    public void close() throws IOException {
      body.close();
    }
  }
}
