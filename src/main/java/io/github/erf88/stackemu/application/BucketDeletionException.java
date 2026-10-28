package io.github.erf88.stackemu.application;

/** Stable bucket failures; uncertain submission never promises that nothing changed. */
public class BucketDeletionException extends RuntimeException {
  public enum Kind {
    NOT_EMPTY,
    UNSAFE,
    NOT_FOUND,
    FORBIDDEN,
    UNAVAILABLE,
    UNCERTAIN
  }

  private final Kind kind;

  public BucketDeletionException(Kind kind) {
    super(kind.name());
    this.kind = kind;
  }

  public Kind kind() {
    return kind;
  }
}
