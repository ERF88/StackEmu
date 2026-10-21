package io.github.erf88.stackemu.application;

/** Stable deletion failures; UNCERTAIN never means that no mutation occurred. */
public class ObjectDeletionException extends RuntimeException {
  public enum Kind {
    UNSAFE,
    NOT_FOUND,
    CONFLICT,
    FORBIDDEN,
    UNAVAILABLE,
    UNCERTAIN
  }

  private final Kind kind;

  public ObjectDeletionException(Kind kind) {
    super(kind.name());
    this.kind = kind;
  }

  public Kind kind() {
    return kind;
  }
}
