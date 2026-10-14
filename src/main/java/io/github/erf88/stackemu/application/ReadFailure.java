package io.github.erf88.stackemu.application;

/** Stable application failure without SDK details or credentials. */
public final class ReadFailure extends RuntimeException {
  public enum Kind {
    NOT_FOUND,
    UNAVAILABLE,
    FORBIDDEN,
    UNSUPPORTED,
    INVALID,
    FAILED
  }

  private final Kind kind;

  public ReadFailure(Kind kind) {
    super(kind.name());
    this.kind = kind;
  }

  public Kind kind() {
    return kind;
  }
}
