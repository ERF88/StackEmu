package io.github.erf88.stackemu.application.sqs;

/** Stable SQS application failure. SDK details and queue URLs never leave the adapter. */
public final class SqsException extends RuntimeException {
  public enum Kind {
    NOT_FOUND,
    INVALID,
    FORBIDDEN,
    CONFLICT,
    UNAVAILABLE,
    FAILED
  }

  private final Kind kind;

  public SqsException(Kind kind) {
    super(kind.name());
    this.kind = kind;
  }

  public Kind kind() {
    return kind;
  }
}
