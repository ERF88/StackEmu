package io.github.erf88.stackemu.adapter.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class S3CliCommandsTest {
  @Test
  void specialKeysArePosixQuotedAndCommandHasNoCredentials() {
    String command =
        S3CliCommands.equivalent(
            "upload", "http://localhost:4566", "us-east-1", "bucket", "a'$(id); ü.txt");
    assertTrue(command.contains("'a'\"'\"'$(id); ü.txt'"));
    assertFalse(command.contains("ACCESS_KEY"));
    assertFalse(command.contains("SECRET"));
    assertTrue(command.contains("--if-none-match '*'"));
  }
}
