package io.github.erf88.stackemu.application;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/** Explicit sharing, version reads and conditional upload replacement. */
public interface S3ObjectToolsPort {
  record SignedDownload(String url, Instant expires) {}

  record Version(String key, String id, boolean marker, boolean latest, Instant modified) {}

  record VersionPage(List<Version> entries, String nextKey, String nextVersion) {}

  SignedDownload signDownload(String bucket, String key, int minutes, String disposition);

  VersionPage versions(String bucket, String key, String keyMarker, String versionMarker);

  /** Permanent version deletion requires an explicitly enabled or suspended bucket. */
  boolean isVersionDeletionAvailable(String bucket);

  String destinationEtag(String bucket, String key);

  void replaceUpload(String bucket, String key, String etag, Path file, String contentType);
}
