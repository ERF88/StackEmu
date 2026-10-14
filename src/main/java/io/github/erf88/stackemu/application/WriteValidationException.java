package io.github.erf88.stackemu.application;

import java.util.Map;

/** Stable field/error keys. Localization belongs to the web adapter. */
public final class WriteValidationException extends RuntimeException {
  private final Map<String, String> errors;

  public WriteValidationException(String field, String error) {
    super("Invalid mutation input");
    errors = Map.of(field, error);
  }

  public Map<String, String> errors() {
    return errors;
  }
}
