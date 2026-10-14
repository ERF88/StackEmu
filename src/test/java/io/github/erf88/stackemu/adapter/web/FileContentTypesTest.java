package io.github.erf88.stackemu.adapter.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FileContentTypesTest {
  @Test
  void encodedClientFilenameIsDecodedBeforeValidation() {
    assertEquals(
        "ação +%.txt", UploadFileNames.originalFilename("=?utf-8?B?YcOnw6NvICslLnR4dA==?="));
    assertEquals("report.pdf", UploadFileNames.originalFilename("report.pdf"));
  }

  @Test
  void reportedTypeIsPreservedAndUnknownTypeFallsBack() {
    assertEquals(
        "application/pdf", FileContentTypes.uploadContentType("report.pdf", "application/pdf"));
    assertEquals(
        "application/pdf",
        FileContentTypes.uploadContentType("report.pdf", "application/octet-stream"));
    assertEquals(
        "application/octet-stream", FileContentTypes.uploadContentType("data.unrecognized", null));
  }

  @Test
  void malformedOrInjectedHeadersCannotBeUsed() {
    for (String type : new String[] {"text/html\r\nX-Foo: injected", "not a type", "*/*"}) {
      assertEquals("application/octet-stream", FileContentTypes.safeContentType(type));
    }
  }

  @Test
  void longUnicodeDownloadNameRetainsExtensionAndAsciiFallback() {
    String header = DownloadHeaders.attachmentDisposition("folder/" + "ü".repeat(140) + ".pdf");
    assertTrue(header.contains(".pdf\""));
    assertTrue(header.endsWith(".pdf"));
    assertTrue(header.contains("filename*=UTF-8''"));
  }
}
