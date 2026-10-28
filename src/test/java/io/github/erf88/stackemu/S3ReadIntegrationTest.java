package io.github.erf88.stackemu;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.erf88.stackemu.adapter.web.S3ViewMapper;
import io.github.erf88.stackemu.application.S3ReadPort;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;

@QuarkusTest
@TestProfile(S3ReadIntegrationTest.Integration.class)
@EnabledIfEnvironmentVariable(named = "STACKEMU_INTEGRATION", matches = "true")
class S3ReadIntegrationTest {
  public static class Integration implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      return Map.of(
          "stackemu.page-size",
          "2",
          "quarkus.s3.api-call-timeout",
          "60S",
          "quarkus.s3.api-call-attempt-timeout",
          "45S");
    }
  }

  @Inject S3Client client;
  @Inject S3ReadPort reader;
  private String bucket;
  private final Set<String> createdKeys = new LinkedHashSet<>();

  @BeforeEach
  void createFixture() {
    bucket = "stackemu-read-" + UUID.randomUUID();
    client.createBucket(request -> request.bucket(bucket));
    createdKeys.clear();
  }

  @AfterEach
  void removeOnlyOwnFixture() {
    for (String key : createdKeys) client.deleteObject(request -> request.bucket(bucket).key(key));
    client.deleteBucket(request -> request.bucket(bucket));
  }

  private void put(String key, String content, String contentType) {
    createdKeys.add(key);
    client.putObject(
        request -> request.bucket(bucket).key(key).contentType(contentType),
        RequestBody.fromString(content));
  }

  @Test
  void objectPanelIsEscapedLocalizedMetadataWithExplicitResponsePolicy() {
    String key = "folder/ação +%<img src=x onerror=alert(1)>.txt";
    put(key, "body must not become inline preview", "text/html");
    given()
        .header("HX-Request", "true")
        .queryParam("bucket", bucket)
        .queryParam("key", key)
        .queryParam("lang", "pt-BR")
        .get("/s3/object-panel")
        .then()
        .statusCode(200)
        .header("X-StackEmu-Fragment", "object-details")
        .header("Cache-Control", "no-store")
        .header("Vary", "HX-Request")
        .header("X-Content-Type-Options", "nosniff")
        .header("Content-Security-Policy", containsString("script-src 'self'"))
        .body(
            containsString("&lt;img src=x onerror=alert(1)&gt;"),
            containsString("Copiar chave do objeto"),
            containsString("/s3/object-metadata?"),
            containsString("text/html"),
            not(containsString("<img")),
            not(containsString("<!doctype")),
            not(containsString("body must not become inline preview")));
  }

  @Test
  void objectPanelOrdinaryNavigationRendersFullPage() {
    put("direct.txt", "fixture", "text/plain");
    given()
        .queryParam("bucket", bucket)
        .queryParam("key", "direct.txt")
        .queryParam("theme", "light")
        .get("/s3/object-panel")
        .then()
        .statusCode(200)
        .header("Cache-Control", "no-store")
        .body(containsString("<!doctype html>"), containsString("data-theme=\"light\""));
  }

  @Test
  void objectPanelErrorsRetainStatusAndNeverBecomeSuccessFragments() {
    given()
        .header("HX-Request", "true")
        .queryParam("bucket", bucket)
        .queryParam("key", "absent.txt")
        .get("/s3/object-panel")
        .then()
        .statusCode(404)
        .header("Cache-Control", "no-store")
        .contentType("text/plain")
        .body(containsString("was not found"));
    given()
        .header("HX-Request", "true")
        .queryParam("bucket", "../invalid")
        .queryParam("key", "key")
        .get("/s3/object-panel")
        .then()
        .statusCode(400)
        .header("Cache-Control", "no-store")
        .contentType("text/plain");
  }

  @Test
  void urlEncodedListingPreservesControlCharactersPrefixesAndLiteralPercentValues() {
    String prefix = "opaque/\r\n+%?ação/";
    String key = prefix + "file +%20.txt";
    put(key, "opaque fixture", "text/plain");
    var root = reader.listObjects(bucket, "opaque/", null, 50);
    assertEquals(List.of(prefix), root.prefixes());
    var listing = reader.listObjects(bucket, prefix, null, 50);
    assertEquals(key, listing.objects().getFirst().key());
    assertEquals(key, reader.getObjectDetails(bucket, key).key());
  }

  @Test
  void moreThanOneThousandObjectsArePagedWithoutDuplicatesOrUnboundedPages() {
    for (int index = 0; index < 1005; index++) {
      put("large/file-" + index + ".txt", "fixture", "text/plain");
    }
    Set<String> seen = new HashSet<>();
    String token = null;
    int pages = 0;
    do {
      var page = reader.listObjects(bucket, "large/", token, 200);
      assertTrue(page.objects().size() <= 200);
      for (var object : page.objects()) {
        assertTrue(seen.add(object.key()), "An object must not repeat across pages");
      }
      token = page.nextToken();
      pages++;
      assertTrue(pages <= 6, "Continuation must terminate for this fixture");
    } while (token != null);
    assertEquals(1005, seen.size());
    assertEquals(6, pages);
  }

  @Test
  void emptyBucketShowsOnlyOperationalContent() {
    given()
        .queryParam("bucket", bucket)
        .get("/s3/bucket")
        .then()
        .statusCode(200)
        .body(
            not(containsString("bucket-metadata")),
            not(containsString("explorer-help")),
            not(containsString("resource-pagination")),
            containsString("No objects or prefixes found here."));
  }

  @Test
  void optionalTagsDoNotAppearInOperationalListing() {
    client.putBucketTagging(
        request ->
            request
                .bucket(bucket)
                .tagging(
                    Tagging.builder()
                        .tagSet(Tag.builder().key("team").value("<b>integration</b>").build())
                        .build()));
    given()
        .queryParam("bucket", bucket)
        .get("/s3/bucket")
        .then()
        .statusCode(200)
        .body(
            not(containsString("&lt;b&gt;integration&lt;/b&gt;")),
            not(containsString("<b>integration</b>")));
  }

  @Test
  void continuationLinksRetrieveEveryObjectWithoutDuplicates() {
    for (int index = 0; index < 5; index++)
      put("page/file-" + index + ".txt", "data", "text/plain");
    String url = S3ViewMapper.bucketPath(bucket, "page/", null) + "&lang=en";
    Set<String> seen = new HashSet<>();
    int pages = 0;
    while (url != null) {
      String html =
          given().urlEncodingEnabled(false).get(url).then().statusCode(200).extract().asString();
      // Count resource entries, not the second Details link among the direct row actions.
      var objects =
          Pattern.compile("class=\"object-name\"><a href=\"(/s3/object\\?[^\"]+)\"").matcher(html);
      int entries = 0;
      while (objects.find()) {
        String link = objects.group(1).replace("&amp;", "&");
        String key = queryValue(link, "key");
        assertTrue(seen.add(key), "An object must not repeat across pages");
        entries++;
      }
      assertTrue(entries <= 2, "Configured page limit must be respected");
      var next = Pattern.compile("id=\"next-page\"[^>]*href=\"([^\"]+)\"").matcher(html);
      url = next.find() ? next.group(1).replace("&amp;", "&") : null;
      pages++;
      assertTrue(pages <= 4, "Continuation must terminate");
    }
    assertEquals(5, seen.size());
    assertEquals(3, pages);
  }

  @Test
  void prefixesPaginateAndBreadcrumbNavigatesThreeLevels() {
    for (String prefix : new String[] {"one/", "two/", "three/"})
      put("groups/" + prefix + "file.txt", "data", "text/plain");
    Set<String> prefixes = new HashSet<>();
    String token = null;
    do {
      var page = reader.listObjects(bucket, "groups/", token, 2);
      assertTrue(page.prefixes().size() + page.objects().size() <= 2);
      prefixes.addAll(page.prefixes());
      token = page.nextToken();
    } while (token != null);
    assertEquals(Set.of("groups/one/", "groups/two/", "groups/three/"), prefixes);
    String key = "level1/level2/level3/ação +%?#.txt";
    put(key, "hello", "text/plain");
    for (String prefix : new String[] {"", "level1/", "level1/level2/", "level1/level2/level3/"}) {
      given()
          .queryParam("bucket", bucket)
          .queryParam("prefix", prefix)
          .queryParam("lang", "pt-BR")
          .get("/s3/bucket")
          .then()
          .statusCode(200)
          .body(containsString("Caminho do bucket e prefixo"));
    }
    String html =
        given()
            .queryParam("bucket", bucket)
            .queryParam("prefix", "level1/level2/level3/")
            .get("/s3/bucket")
            .then()
            .statusCode(200)
            .extract()
            .asString();
    assertTrue(html.contains("prefix=level1%2F"));
    assertTrue(html.contains("prefix=level1%2Flevel2%2F"));
    assertTrue(html.contains("prefix=level1%2Flevel2%2Flevel3%2F"));
    String objectUrl = S3ViewMapper.objectPath(bucket, key);
    given()
        .urlEncodingEnabled(false)
        .get(objectUrl)
        .then()
        .statusCode(200)
        .body(
            containsString("ação +%?#.txt"),
            containsString("text/plain"),
            containsString("/s3/object-metadata?"));
  }

  @Test
  void specialKeysStayInertAndDownloadIsAnAttachment() {
    String key = "<img src=x onerror=alert(1)> ü +%?#.html";
    String content = "<script>alert('object')</script>";
    put(key, content, "text/html");
    given()
        .queryParam("bucket", bucket)
        .get("/s3/bucket")
        .then()
        .statusCode(200)
        .body(containsString("&lt;img"), not(containsString("<img")));
    given()
        .urlEncodingEnabled(false)
        .get(S3ViewMapper.objectPath(bucket, key))
        .then()
        .statusCode(200)
        .body(containsString("&lt;img"), containsString("text/html"), not(containsString("<img")));
    given()
        .urlEncodingEnabled(false)
        .get(S3ViewMapper.downloadPath(bucket, key))
        .then()
        .statusCode(200)
        .contentType("text/html")
        .header("X-Content-Type-Options", "nosniff")
        .header("Content-Disposition", containsString("attachment;"))
        .body(org.hamcrest.Matchers.equalTo(content));
  }

  @Test
  void missingResourcesAndInvalidInputHaveSafeStatusCodes() {
    given()
        .queryParam("bucket", "stackemu-absent-" + UUID.randomUUID())
        .get("/s3/bucket")
        .then()
        .statusCode(404)
        .body(containsString("not found"), not(containsString("S3Exception")));
    given()
        .queryParam("bucket", bucket)
        .queryParam("key", "missing")
        .get("/s3/object")
        .then()
        .statusCode(404);
    given()
        .queryParam("bucket", bucket)
        .queryParam("key", "missing")
        .get("/s3/download")
        .then()
        .statusCode(404)
        .contentType("text/plain");
    given().get("/s3/bucket").then().statusCode(400);
    given()
        .queryParam("bucket", bucket)
        .queryParam("prefix", "a".repeat(1025))
        .get("/s3/bucket")
        .then()
        .statusCode(400);
  }

  @Test
  void largeDownloadHasIdenticalSizeAndSha256WithoutClientBuffering() throws Exception {
    var source = Files.createTempFile("stackemu-source-", ".bin");
    var destination = Files.createTempFile("stackemu-download-", ".bin");
    String key = "large.bin";
    try {
      byte[] chunk = new byte[64 * 1024];
      new java.util.Random(42).nextBytes(chunk);
      try (var output = Files.newOutputStream(source)) {
        for (int index = 0; index < 512; index++) output.write(chunk);
      }
      createdKeys.add(key);
      client.putObject(
          request -> request.bucket(bucket).key(key).contentType("application/octet-stream"),
          RequestBody.fromFile(source));
      URI uri =
          URI.create(
              "http://127.0.0.1:"
                  + io.restassured.RestAssured.port
                  + S3ViewMapper.downloadPath(bucket, key));
      var response =
          HttpClient.newHttpClient()
              .send(
                  HttpRequest.newBuilder(uri)
                      .timeout(java.time.Duration.ofSeconds(60))
                      .GET()
                      .build(),
                  HttpResponse.BodyHandlers.ofFile(destination));
      assertEquals(200, response.statusCode());
      assertEquals(32L * 1024 * 1024, Files.size(destination));
      assertEquals(hash(source), hash(destination));
    } finally {
      Files.deleteIfExists(source);
      Files.deleteIfExists(destination);
    }
  }

  private static String queryValue(String path, String name) {
    for (String part : URI.create(path).getRawQuery().split("&")) {
      String[] pair = part.split("=", 2);
      if (pair[0].equals(name)) return URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
    }
    throw new AssertionError("Missing query value");
  }

  private static String hash(java.nio.file.Path file) throws Exception {
    var digest = MessageDigest.getInstance("SHA-256");
    try (InputStream input = Files.newInputStream(file)) {
      byte[] buffer = new byte[64 * 1024];
      int count;
      while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
    }
    return java.util.HexFormat.of().formatHex(digest.digest());
  }
}
