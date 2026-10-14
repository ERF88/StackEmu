package io.github.erf88.stackemu.application;

/** Safe mutation failure category; never retain SDK response details. */
public final class S3WriteException extends RuntimeException {
  public enum Kind {
    CONFLICT,
    NOT_FOUND,
    FORBIDDEN,
    UNAVAILABLE,
    FAILED
  }

  private final Kind kind;

  public S3WriteException(Kind kind) {
    super(kind.name());
    this.kind = kind;
  }

  public Kind kind() {
    return kind;
  }
}
