package io.github.erf88.stackemu.application;

import java.nio.file.Path;

/** Mutation boundary. Upload paths are server-generated temporary files owned by the caller. */
public interface S3WritePort {
  void createBucket(String bucket);

  /** Creates a new object only; an existing key must cause a conflict, never an overwrite. */
  void uploadObject(String bucket, String key, Path file);

  /** Creates a zero-byte key ending in slash; it is a marker, not a filesystem directory. */
  void createFolderMarker(String bucket, String prefix);
}
