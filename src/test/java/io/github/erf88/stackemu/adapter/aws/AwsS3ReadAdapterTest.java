package io.github.erf88.stackemu.adapter.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.erf88.stackemu.application.ReadFailure;
import io.github.erf88.stackemu.application.S3ReadPort;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Exception;

class AwsS3ReadAdapterTest {
  @Test
  void absentTagsAndUnsupportedOptionalRegionDoNotFailBucket() {
    var adapter = new AwsS3ReadAdapter();
    adapter.client =
        fake(
            (proxy, method, args) ->
                switch (method.getName()) {
                  case "headBucket" -> HeadBucketResponse.builder().build();
                  case "getBucketLocation" -> throw S3Exception.builder().statusCode(501).build();
                  case "getBucketTagging" ->
                      throw S3Exception.builder()
                          .statusCode(404)
                          .awsErrorDetails(
                              AwsErrorDetails.builder().errorCode("NoSuchTagSet").build())
                          .build();
                  default -> throw new UnsupportedOperationException(method.getName());
                });
    assertEquals(S3ReadPort.TagsState.EMPTY, adapter.bucketDetails("bucket").tagsState());
  }

  @Test
  void missingContinuationTokenCannotSilentlyHideMoreResults() {
    var adapter = new AwsS3ReadAdapter();
    adapter.client =
        fake((proxy, method, args) -> ListObjectsV2Response.builder().isTruncated(true).build());
    var failure =
        assertThrows(ReadFailure.class, () -> adapter.listObjects("bucket", "", null, 50));
    assertEquals(ReadFailure.Kind.FAILED, failure.kind());
  }

  @Test
  void deniedAndMissingRequiredResourcesAreTranslated() {
    var adapter = new AwsS3ReadAdapter();
    adapter.client =
        fake(
            (proxy, method, args) -> {
              throw S3Exception.builder().statusCode(403).message("internal").build();
            });
    var failure = assertThrows(ReadFailure.class, () -> adapter.bucketDetails("bucket"));
    assertEquals(ReadFailure.Kind.FORBIDDEN, failure.kind());
    assertEquals("FORBIDDEN", failure.getMessage());
  }

  private static S3Client fake(java.lang.reflect.InvocationHandler handler) {
    return (S3Client)
        Proxy.newProxyInstance(
            S3Client.class.getClassLoader(), new Class<?>[] {S3Client.class}, handler);
  }
}
