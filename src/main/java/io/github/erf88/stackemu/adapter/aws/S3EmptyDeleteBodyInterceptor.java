package io.github.erf88.stackemu.adapter.aws;

import java.util.Optional;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

/** Enables URLConnection fixed-length mode; SDK retry settings alone do not prevent replay. */
final class S3EmptyDeleteBodyInterceptor implements ExecutionInterceptor {
  @Override
  public Optional<RequestBody> modifyHttpContent(
      Context.ModifyHttpRequest context, ExecutionAttributes attributes) {
    return context.request() instanceof DeleteObjectRequest
            || context.request() instanceof DeleteBucketRequest
        ? Optional.of(RequestBody.empty())
        : context.requestBody();
  }
}
