package io.github.erf88.stackemu.adapter.web;

/** Illustrative POSIX-shell commands only; never executed and never include credentials. */
public final class S3CliCommands {
  private S3CliCommands() {}

  public static String equivalent(
      String action, String endpoint, String region, String bucket, String key) {
    return equivalent(action, endpoint, region, bucket, key, "application/octet-stream");
  }

  public static String equivalent(
      String action,
      String endpoint,
      String region,
      String bucket,
      String key,
      String contentType) {
    String base =
        "aws --endpoint-url " + quote(endpoint) + " --region " + quote(region) + " s3api ";
    return switch (action) {
      case "bucket" ->
          base
              + "create-bucket --bucket "
              + quote(bucket)
              + ("us-east-1".equals(region)
                  ? ""
                  : " --create-bucket-configuration " + quote("LocationConstraint=" + region));
      case "folder" ->
          base
              + "put-object --bucket "
              + quote(bucket)
              + " --key "
              + quote(key)
              + " --if-none-match '*'";
      default ->
          base
              + "put-object --bucket "
              + quote(bucket)
              + " --key "
              + quote(key)
              + " --body '/path/to/file' --content-type "
              + quote(contentType)
              + " --if-none-match '*'";
    };
  }

  private static String quote(String value) {
    if (value == null || value.codePoints().anyMatch(Character::isISOControl)) {
      return "'<invalid>'";
    }
    return "'" + value.replace("'", "'\"'\"'") + "'";
  }
}
