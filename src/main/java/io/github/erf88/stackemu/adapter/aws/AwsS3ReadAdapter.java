package io.github.erf88.stackemu.adapter.aws;

import io.github.erf88.stackemu.application.S3ReadException;
import io.github.erf88.stackemu.application.S3ReadPort;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.FilterInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ApplicationScoped
public class AwsS3ReadAdapter implements S3ReadPort {
  private final S3Client client;

  public AwsS3ReadAdapter(S3Client client) {
    this.client = client;
  }

  @Override
  public BucketDetails getBucketDetails(String bucket) {
    try {
      client.headBucket(request -> request.bucket(bucket));
    } catch (SdkException failure) {
      throw translate(failure);
    }
    String reportedRegion = null;
    Map<String, String> tags = new TreeMap<>();
    TagsState tagsState = TagsState.UNKNOWN;
    try {
      reportedRegion =
          client.getBucketLocation(request -> request.bucket(bucket)).locationConstraintAsString();
    } catch (SdkException ignored) {
      // Optional metadata failure does not make a readable bucket unusable.
    }
    try {
      var response = client.getBucketTagging(request -> request.bucket(bucket));
      for (var tag : response.tagSet()) {
        if (tag.key() != null && tag.value() != null) tags.put(tag.key(), tag.value());
      }
      tagsState = tags.isEmpty() ? TagsState.EMPTY : TagsState.AVAILABLE;
    } catch (S3Exception failure) {
      if (failure.awsErrorDetails() != null
          && "NoSuchTagSet".equals(failure.awsErrorDetails().errorCode())) {
        tagsState = TagsState.EMPTY;
      }
    } catch (SdkException ignored) {
      // Unknown optional support is distinct from an empty tag set.
    }
    return new BucketDetails(bucket, reportedRegion, tags, tagsState);
  }

  @Override
  public ObjectPage listObjects(String bucket, String prefix, String token, int pageSize) {
    try {
      var response =
          client.listObjectsV2(
              request ->
                  request
                      .bucket(bucket)
                      .prefix(prefix)
                      .delimiter("/")
                      .continuationToken(token)
                      .maxKeys(pageSize));
      if (Boolean.TRUE.equals(response.isTruncated())
          && (response.nextContinuationToken() == null
              || response.nextContinuationToken().isEmpty())) {
        throw new S3ReadException(S3ReadException.Kind.FAILED);
      }
      return new ObjectPage(
          response.commonPrefixes().stream().map(p -> p.prefix()).toList(),
          response.contents().stream()
              .map(
                  o ->
                      new ObjectSummary(
                          o.key(), o.size() == null ? -1 : o.size(), o.lastModified()))
              .toList(),
          Boolean.TRUE.equals(response.isTruncated()) ? response.nextContinuationToken() : null);
    } catch (SdkException failure) {
      throw translate(failure);
    }
  }

  @Override
  public ObjectDetails getObjectDetails(String bucket, String key) {
    try {
      var response = client.headObject(request -> request.bucket(bucket).key(key));
      return new ObjectDetails(
          key,
          response.contentLength() == null ? -1 : response.contentLength(),
          response.contentType(),
          response.eTag(),
          response.lastModified());
    } catch (SdkException failure) {
      throw translate(failure);
    }
  }

  @Override
  public Download openDownload(String bucket, String key) {
    try {
      var response = client.getObject(request -> request.bucket(bucket).key(key));
      var stream =
          new FilterInputStream(response) {
            @Override
            public void close() throws IOException {
              // Abort avoids draining unread content when the browser cancels a download.
              response.abort();
              response.close();
            }
          };
      return new Download(
          stream,
          response.response().contentLength() == null ? -1 : response.response().contentLength(),
          response.response().contentType());
    } catch (SdkException failure) {
      throw translate(failure);
    }
  }

  private static S3ReadException translate(SdkException failure) {
    if (failure instanceof SdkClientException)
      return new S3ReadException(S3ReadException.Kind.UNAVAILABLE);
    if (failure instanceof S3Exception response) {
      return new S3ReadException(
          switch (response.statusCode()) {
            case 404 -> S3ReadException.Kind.NOT_FOUND;
            case 403 -> S3ReadException.Kind.FORBIDDEN;
            case 400 -> S3ReadException.Kind.INVALID;
            case 501 -> S3ReadException.Kind.UNSUPPORTED;
            default -> S3ReadException.Kind.FAILED;
          });
    }
    return new S3ReadException(S3ReadException.Kind.FAILED);
  }
}
