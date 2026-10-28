package io.github.erf88.stackemu.application;

/** Stable reasons, translated at the HTTP boundary. */
public class OperationException extends RuntimeException {
  public enum Reason {
    MISSING,
    CHANGED,
    FORBIDDEN,
    UNAVAILABLE,
    UNCERTAIN,
    CONFLICT,
    UNSUPPORTED,
    CAPACITY,
    NAME,
    COPIED_SOURCE_RETAINED
  }

  private final Reason reason;

  public OperationException(Reason reason) {
    super(reason.name());
    this.reason = reason;
  }

  public OperationException(Reason reason, Throwable cause) {
    super(reason.name(), cause);
    this.reason = reason;
  }

  public Reason reason() {
    return reason;
  }
}
