package io.github.erf88.stackemu.adapter.aws;

import io.github.erf88.stackemu.application.ImagePreviewPolicy;
import io.github.erf88.stackemu.application.S3ImagePreviewPort;
import io.github.erf88.stackemu.application.S3PreviewPort;
import io.github.erf88.stackemu.application.S3ReadException;
import io.github.erf88.stackemu.application.TextPreviewPolicy;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ApplicationScoped
public class AwsS3PreviewAdapter implements S3PreviewPort, S3ImagePreviewPort, AutoCloseable {
  private final S3Client client;
  private final RasterPreviewProcess raster;

  public AwsS3PreviewAdapter(S3Client configuredClient, RasterPreviewProcess raster) {
    this.raster = raster;
    var settings = configuredClient.serviceClientConfiguration();
    var builder =
        S3Client.builder()
            .endpointOverride(settings.endpointOverride().orElseThrow())
            .region(settings.region())
            .credentialsProvider(settings.credentialsProvider())
            .overrideConfiguration(settings.overrideConfiguration())
            .forcePathStyle(true);
    this.client =
        builder
            .httpClientBuilder(
                UrlConnectionHttpClient.builder()
                    .connectionTimeout(Duration.ofSeconds(1))
                    .socketTimeout(Duration.ofSeconds(2)))
            .build();
  }

  @Override
  public ImagePreview readImagePreview(String bucket, String key) {
    ImageInput input;
    var request =
        GetObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .overrideConfiguration(
                options ->
                    options
                        .apiCallTimeout(Duration.ofSeconds(3))
                        .apiCallAttemptTimeout(Duration.ofSeconds(2)))
            .build();
    try {
      input = client.getObject(request, (metadata, body) -> readImageBody(key, metadata, body));
    } catch (SdkClientException failure) {
      throw new S3ReadException(S3ReadException.Kind.UNAVAILABLE);
    } catch (S3Exception failure) {
      throw new S3ReadException(
          switch (failure.statusCode()) {
            case 404 -> S3ReadException.Kind.NOT_FOUND;
            case 403 -> S3ReadException.Kind.FORBIDDEN;
            case 400 -> S3ReadException.Kind.INVALID;
            case 501 -> S3ReadException.Kind.UNSUPPORTED;
            default -> S3ReadException.Kind.FAILED;
          });
    } catch (SdkException failure) {
      throw new S3ReadException(S3ReadException.Kind.FAILED);
    }
    if (input.state() != ImageState.AVAILABLE) return new ImagePreview(input.state(), new byte[0]);
    try {
      return new ImagePreview(ImageState.AVAILABLE, raster.reencode(input.bytes(), input.format()));
    } catch (RasterPreviewProcess.WorkerException refused) {
      return new ImagePreview(ImageState.INVALID_IMAGE, new byte[0]);
    } catch (InterruptedException failure) {
      Thread.currentThread().interrupt();
      throw new S3ReadException(S3ReadException.Kind.UNAVAILABLE);
    } catch (IOException failure) {
      throw new S3ReadException(S3ReadException.Kind.UNAVAILABLE);
    }
  }

  record ImageInput(ImageState state, String format, byte[] bytes) {}

  static ImageInput readImageBody(String key, GetObjectResponse metadata, AbortableInputStream body)
      throws IOException, InterruptedException {
    try {
      String format =
          ImagePreviewPolicy.format(key, metadata.contentType(), metadata.contentEncoding());
      if (format.isEmpty() || metadata.contentRange() != null)
        return new ImageInput(ImageState.UNSUPPORTED, "", new byte[0]);
      Long length = metadata.contentLength();
      if (length != null && length > ImagePreviewPolicy.MAX_BYTES)
        return new ImageInput(ImageState.TOO_LARGE, "", new byte[0]);
      long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
      var output = new ByteArrayOutputStream();
      byte[] buffer = new byte[4096];
      while (output.size() <= ImagePreviewPolicy.MAX_BYTES) {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
        if (System.nanoTime() >= deadline) throw new IOException("Image body deadline");
        int read =
            body.read(
                buffer,
                0,
                Math.min(buffer.length, ImagePreviewPolicy.MAX_BYTES + 1 - output.size()));
        if (read < 0) break;
        output.write(buffer, 0, read);
      }
      if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
      if (System.nanoTime() >= deadline) throw new IOException("Image body deadline");
      if (output.size() > ImagePreviewPolicy.MAX_BYTES)
        return new ImageInput(ImageState.TOO_LARGE, "", new byte[0]);
      if (length != null && length >= 0 && length != output.size())
        throw new IOException("Incomplete image");
      return new ImageInput(
          output.size() == 0 ? ImageState.INVALID_IMAGE : ImageState.AVAILABLE,
          format,
          output.toByteArray());
    } finally {
      body.abort();
    }
  }

  @PreDestroy
  @Override
  public void close() {
    client.close();
  }

  @Override
  public TextPreview readTextPreview(String bucket, String key) {
    var request =
        GetObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .overrideConfiguration(
                options ->
                    options
                        .apiCallTimeout(Duration.ofSeconds(3))
                        .apiCallAttemptTimeout(Duration.ofSeconds(2)))
            .build();
    try {
      // A managed transformer keeps body consumption inside the SDK operation's timeout.
      return client.getObject(request, (metadata, body) -> readBody(key, metadata, body));
    } catch (SdkClientException failure) {
      throw new S3ReadException(S3ReadException.Kind.UNAVAILABLE);
    } catch (S3Exception failure) {
      throw new S3ReadException(
          switch (failure.statusCode()) {
            case 404 -> S3ReadException.Kind.NOT_FOUND;
            case 403 -> S3ReadException.Kind.FORBIDDEN;
            case 400 -> S3ReadException.Kind.INVALID;
            case 501 -> S3ReadException.Kind.UNSUPPORTED;
            default -> S3ReadException.Kind.FAILED;
          });
    } catch (SdkException failure) {
      throw new S3ReadException(S3ReadException.Kind.FAILED);
    }
  }

  static TextPreview readBody(String key, GetObjectResponse metadata, AbortableInputStream body)
      throws IOException, InterruptedException {
    try {
      if (!TextPreviewPolicy.supports(key, metadata.contentType(), metadata.contentEncoding())
          || metadata.contentRange() != null)
        return TextPreviewPolicy.refused(PreviewState.UNSUPPORTED);
      Long length = metadata.contentLength();
      if (length != null && length > TextPreviewPolicy.MAX_BYTES)
        return TextPreviewPolicy.refused(PreviewState.TOO_LARGE);
      if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
      long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
      var output = new ByteArrayOutputStream();
      byte[] buffer = new byte[4096];
      while (output.size() <= TextPreviewPolicy.MAX_BYTES) {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
        if (System.nanoTime() >= deadline) throw new IOException("Preview body deadline exceeded");
        int read =
            body.read(
                buffer,
                0,
                Math.min(buffer.length, TextPreviewPolicy.MAX_BYTES + 1 - output.size()));
        if (read < 0) break;
        output.write(buffer, 0, read);
      }
      if (System.nanoTime() >= deadline) throw new IOException("Preview body deadline exceeded");
      byte[] bytes = output.toByteArray();
      if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
      if (bytes.length > TextPreviewPolicy.MAX_BYTES)
        return TextPreviewPolicy.refused(PreviewState.TOO_LARGE);
      if (length != null && length >= 0 && length != bytes.length)
        throw new IOException("Incomplete preview transfer");
      return TextPreviewPolicy.decode(bytes);
    } finally {
      // Never let the SDK drain an oversized, rejected or interrupted object.
      body.abort();
    }
  }
}
