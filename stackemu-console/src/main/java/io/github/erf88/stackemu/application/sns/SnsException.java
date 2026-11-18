package io.github.erf88.stackemu.application.sns;

/** Stable SNS application failure; provider details stay inside the adapter. */
public final class SnsException extends RuntimeException {
  public enum Kind {
    NOT_FOUND,
    INVALID,
    FORBIDDEN,
    CONFLICT,
    UNAVAILABLE,
    FAILED
  }

  private final Kind kind;

  public SnsException(Kind kind) {
    super(kind.name());
    this.kind = kind;
  }

  public Kind kind() {
    return kind;
  }
}
