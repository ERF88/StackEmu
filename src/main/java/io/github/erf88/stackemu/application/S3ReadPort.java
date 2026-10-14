package io.github.erf88.stackemu.application;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Read-only S3 boundary; optional metadata may be absent. */
public interface S3ReadPort {
  BucketDetails bucketDetails(String bucket);

  ObjectPage listObjects(String bucket, String prefix, String continuationToken, int pageSize);

  ObjectDetails objectDetails(String bucket, String key);

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
  record Download(InputStream body, long size) implements AutoCloseable {
    @Override
    public void close() throws IOException {
      body.close();
    }
  }
}
