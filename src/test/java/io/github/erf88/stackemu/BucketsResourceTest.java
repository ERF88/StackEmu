package io.github.erf88.stackemu;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(BucketsResourceTest.Unavailable.class)
class BucketsResourceTest {
  @jakarta.inject.Inject software.amazon.awssdk.services.s3.S3Client client;

  public static class Unavailable implements QuarkusTestProfile {
    public Map<String, String> getConfigOverrides() {
      return Map.of(
          "quarkus.s3.endpoint-override",
          "http://127.0.0.1:1",
          "quarkus.s3.aws.region",
          "sa-east-1",
          "quarkus.s3.aws.credentials.static-provider.secret-access-key",
          "hidden-test-value");
    }
  }

  @Test
  void unavailableIsAnUnderstandablePage() {
    given()
        .get("/")
        .then()
        .statusCode(200)
        .body(containsString("Emulator unavailable"), not(containsString("SdkException")));
  }

  @Test
  void partialUpdateIsLocalizedAndNotAFullDocument() {
    given()
        .queryParam("lang", "pt-BR")
        .header("HX-Request", "true")
        .get("/buckets")
        .then()
        .statusCode(200)
        .body(containsString("Emulador indisponível"), not(containsString("<!doctype")));
  }

  @Test
  void processHealthDoesNotDependOnEmulator() {
    given().get("/q/health/live").then().statusCode(200);
  }

  @Test
  void connectionShowsEffectiveConfigurationWithoutSecrets() {
    org.junit.jupiter.api.Assertions.assertEquals(
        "sa-east-1", client.serviceClientConfiguration().region().id());
    given()
        .get("/connection")
        .then()
        .statusCode(200)
        .body(
            containsString("http://127.0.0.1:1"),
            containsString("sa-east-1"),
            containsString("Emulator unavailable"),
            not(containsString("hidden-test-value")));
  }

  @Test
  void galleryRendersWithoutEmulatorAndPreservesPreferences() {
    given()
        .queryParam("lang", "pt-BR")
        .queryParam("theme", "light")
        .get("/components")
        .then()
        .statusCode(200)
        .body(
            containsString("Galeria de componentes"),
            containsString("data-theme=\"light\""),
            containsString("lang=pt-BR"));
  }

  @Test
  void languageNegotiationRespectsExplicitOverride() {
    given()
        .header("Accept-Language", "pt-BR, en;q=0.8")
        .get("/connection")
        .then()
        .statusCode(200)
        .body(containsString("Endpoint efetivo"));
    given()
        .header("Accept-Language", "pt-BR")
        .queryParam("lang", "en")
        .get("/connection")
        .then()
        .statusCode(200)
        .body(containsString("Effective endpoint"));
  }

  @Test
  void s3ReadFailureKeepsLocalizedPageAndHidesSecrets() {
    given()
        .queryParam("bucket", "bucket")
        .queryParam("lang", "pt-BR")
        .get("/s3/bucket")
        .then()
        .statusCode(503)
        .body(
            containsString("Emulador indisponível"),
            not(containsString("hidden-test-value")),
            not(containsString("SdkException")));
  }

  @Test
  void downloadFailureBeforeStreamingIsSafePlainText() {
    given()
        .queryParam("bucket", "bucket")
        .queryParam("key", "file")
        .get("/s3/download")
        .then()
        .statusCode(503)
        .contentType("text/plain")
        .body(containsString("Emulator unavailable"), not(containsString("hidden-test-value")));
  }
}
