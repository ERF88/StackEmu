package io.github.erf88.stackemu;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

@QuarkusTest
@TestProfile(S3ReadIntegrationTest.Integration.class)
@EnabledIfEnvironmentVariable(named = "STACKEMU_INTEGRATION", matches = "true")
class S3ImagePreviewIntegrationTest {
  @Inject S3Client client;
  private String bucket;
  private final Set<String> keys = new LinkedHashSet<>();

  @BeforeEach
  void createFixture() {
    bucket = "stackemu-image-" + UUID.randomUUID();
    client.createBucket(request -> request.bucket(bucket));
    keys.clear();
  }

  @AfterEach
  void cleanupOnlyOwnFixtures() {
    for (String key : keys) client.deleteObject(request -> request.bucket(bucket).key(key));
    client.deleteBucket(request -> request.bucket(bucket));
  }

  private void put(String key, byte[] bytes, String type, String encoding) {
    keys.add(key);
    client.putObject(
        request -> request.bucket(bucket).key(key).contentType(type).contentEncoding(encoding),
        RequestBody.fromBytes(bytes));
  }

  @Test
  void opaqueNativePageAndSanitizedPngKeepAttachmentOriginal() throws Exception {
    String key = "folder/ação +%<img src=x>.PNG";
    byte[] original =
        chunk(
            fixture("png", 3, 2),
            "tEXt",
            "Comment\0source-private-marker".getBytes(StandardCharsets.UTF_8));
    put(key, original, "image/png", null);
    given()
        .queryParam("bucket", bucket)
        .queryParam("key", key)
        .queryParam("lang", "pt-BR")
        .get("/s3/object")
        .then()
        .statusCode(200)
        .body(containsString("Prévia de imagem"));
    given()
        .queryParam("bucket", bucket)
        .queryParam("key", key)
        .queryParam("lang", "pt-BR")
        .get("/s3/preview/image")
        .then()
        .statusCode(200)
        .header("Cache-Control", "no-store")
        .body(
            containsString("Prévia de imagem"),
            containsString("&lt;img"),
            containsString("a%C3%A7%C3%A3o%20%2B%25%3Cimg"),
            containsString("/s3/download?"),
            not(containsString("<img src=x>")));
    byte[] png =
        given()
            .queryParam("bucket", bucket)
            .queryParam("key", key)
            .get("/s3/preview/image/content")
            .then()
            .statusCode(200)
            .contentType("image/png")
            .header("Content-Disposition", "inline; filename=\"preview.png\"")
            .header("Cache-Control", "no-store")
            .header("X-Content-Type-Options", "nosniff")
            .header("Referrer-Policy", "no-referrer")
            .header("X-Frame-Options", "DENY")
            .header("Content-Security-Policy", containsString("img-src 'self'"))
            .extract()
            .asByteArray();
    assertFalse(new String(png, StandardCharsets.ISO_8859_1).contains("source-private-marker"));
    BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
    assertNotNull(image);
    assertEquals(3, image.getWidth());
    assertEquals(2, image.getHeight());
    assertArrayEquals(
        original,
        given()
            .queryParam("bucket", bucket)
            .queryParam("key", key)
            .get("/s3/download")
            .then()
            .statusCode(200)
            .contentType("image/png")
            .header("Content-Disposition", containsString("attachment"))
            .extract()
            .asByteArray());
  }

  @Test
  void jpegIsReencodedAsPngWhileOriginalRetainsJpeg() throws Exception {
    byte[] jpeg = fixture("jpeg", 4, 3);
    put("photo.JPEG", jpeg, "Image/Jpeg", null);
    byte[] result =
        given()
            .queryParam("bucket", bucket)
            .queryParam("key", "photo.JPEG")
            .get("/s3/preview/image/content")
            .then()
            .statusCode(200)
            .contentType("image/png")
            .extract()
            .asByteArray();
    assertEquals(137, Byte.toUnsignedInt(result[0]));
    assertArrayEquals(
        jpeg,
        given()
            .queryParam("bucket", bucket)
            .queryParam("key", "photo.JPEG")
            .get("/s3/download")
            .then()
            .statusCode(200)
            .contentType("image/jpeg")
            .extract()
            .asByteArray());
  }

  @Test
  void metadataSizeMalformedAnimationAndSuffixRefusalsNeverReturnImageBytes() throws Exception {
    byte[] png = fixture("png", 2, 2);
    byte[] suffix = Arrays.copyOf(png, png.length + 1);
    byte[] badCrc = png.clone();
    badCrc[29] ^= 1;
    byte[] jpeg = fixture("jpeg", 2, 2);
    byte[] jpegSuffix = Arrays.copyOf(jpeg, jpeg.length + 1);
    record Refusal(String key, String type, String encoding, byte[] bytes, int status) {}
    for (var fixture :
        new Refusal[] {
          new Refusal(
              "a.svg", "image/svg+xml", null, "SECRET".getBytes(StandardCharsets.UTF_8), 415),
          new Refusal("generic.png", "application/octet-stream", null, png, 415),
          new Refusal("gzip.png", "image/png", "gzip", png, 415),
          new Refusal("large.png", "image/png", null, new byte[1024 * 1024 + 1], 413),
          new Refusal("empty.png", "image/png", null, new byte[0], 422),
          new Refusal("mismatch.png", "image/png", null, jpeg, 422),
          new Refusal("short.png", "image/png", null, Arrays.copyOf(png, 24), 422),
          new Refusal("suffix.png", "image/png", null, suffix, 422),
          new Refusal("crc.png", "image/png", null, badCrc, 422),
          new Refusal("animated.png", "image/png", null, chunk(png, "acTL", new byte[8]), 422),
          new Refusal("suffix.jpg", "image/jpeg", null, jpegSuffix, 422),
          new Refusal("short.jpg", "image/jpeg", null, Arrays.copyOf(jpeg, jpeg.length - 2), 422),
          new Refusal("wide.png", "image/png", null, fixture("png", 4097, 1), 422)
        }) {
      put(fixture.key(), fixture.bytes(), fixture.type(), fixture.encoding());
      given()
          .queryParam("bucket", bucket)
          .queryParam("key", fixture.key())
          .queryParam("lang", "pt-BR")
          .get("/s3/preview/image/content")
          .then()
          .statusCode(fixture.status())
          .contentType("text/plain")
          .header("Cache-Control", "no-store")
          .body(containsString("Prévia"), not(containsString("SECRET")));
    }
  }

  @Test
  void shellDoesNotClaimExistenceAndContentRetainsRealMissingOrInvalidStatus() {
    given()
        .queryParam("bucket", bucket)
        .queryParam("key", "missing.png")
        .get("/s3/preview/image")
        .then()
        .statusCode(200)
        .body(containsString("does not confirm object availability"));
    given()
        .queryParam("bucket", bucket)
        .queryParam("key", "missing.png")
        .get("/s3/preview/image/content")
        .then()
        .statusCode(404)
        .header("Cache-Control", "no-store");
    for (String route : new String[] {"/s3/preview/image", "/s3/preview/image/content"})
      given()
          .queryParam("bucket", "http://external.invalid")
          .queryParam("key", "a.png")
          .get(route)
          .then()
          .statusCode(400)
          .header("Cache-Control", "no-store");
  }

  private static byte[] fixture(String format, int width, int height) throws Exception {
    var output = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), format, output);
    return output.toByteArray();
  }

  private static byte[] chunk(byte[] png, String type, byte[] data) {
    byte[] name = type.getBytes(StandardCharsets.US_ASCII);
    CRC32 crc = new CRC32();
    crc.update(name);
    crc.update(data);
    var inserted =
        ByteBuffer.allocate(12 + data.length)
            .putInt(data.length)
            .put(name)
            .put(data)
            .putInt((int) crc.getValue())
            .array();
    // Insert after IHDR, leaving the real IDAT/IEND intact.
    var output = new ByteArrayOutputStream();
    output.writeBytes(Arrays.copyOfRange(png, 0, 33));
    output.writeBytes(inserted);
    output.writeBytes(Arrays.copyOfRange(png, 33, png.length));
    return output.toByteArray();
  }
}
