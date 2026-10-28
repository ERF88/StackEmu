package io.github.erf88.stackemu.adapter.aws;

import io.github.erf88.stackemu.application.ObjectDeletionException;
import io.github.erf88.stackemu.application.ObjectDeletionException.Kind;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;

/** Shared bounded wire proof for deletion inventories; never trust coerced XML booleans. */
final class S3DeletionListingInterceptor implements ExecutionInterceptor {
  private final int maximumResponseBytes;

  S3DeletionListingInterceptor() {
    this(65_536);
  }

  S3DeletionListingInterceptor(int maximumResponseBytes) {
    this.maximumResponseBytes = maximumResponseBytes;
  }

  @Override
  public Optional<InputStream> modifyHttpResponseContent(
      Context.ModifyHttpResponse context, ExecutionAttributes attributes) {
    boolean versions = context.request() instanceof ListObjectVersionsRequest;
    if (!(context.request() instanceof ListObjectsV2Request) && !versions
        || !context.httpResponse().isSuccessful()) return context.responseBody();
    try (var original = context.responseBody().orElseThrow()) {
      byte[] bytes = original.readNBytes(maximumResponseBytes + 1);
      if (bytes.length > maximumResponseBytes) throw new ObjectDeletionException(Kind.UNSAFE);
      var factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      var document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
      var root = document.getDocumentElement();
      var fields = root.getElementsByTagNameNS("*", "IsTruncated");
      if (!(versions ? "ListVersionsResult" : "ListBucketResult").equals(root.getLocalName())
          || fields.getLength() != 1
          || fields.item(0).getParentNode() != root
          || fields.item(0).getChildNodes().getLength() != 1
          || !("false".equals(fields.item(0).getTextContent())
              || "true".equals(fields.item(0).getTextContent()))) {
        throw new ObjectDeletionException(Kind.UNSAFE);
      }
      return Optional.of(new ByteArrayInputStream(bytes));
    } catch (IOException | SAXException | ParserConfigurationException invalid) {
      throw new ObjectDeletionException(Kind.UNSAFE);
    }
  }
}
