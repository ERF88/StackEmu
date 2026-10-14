package io.github.erf88.stackemu;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import java.util.Map;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(BucketsResourceTest.Unavailable.class)
class BucketsResourceTest {
    public static class Unavailable implements QuarkusTestProfile {
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.s3.endpoint-override", "http://127.0.0.1:1");
        }
    }
    @Test void unavailableIsAnUnderstandablePage() {
        given().get("/").then().statusCode(200)
            .body(containsString("Emulator unavailable"), not(containsString("SdkException")));
    }
    @Test void partialUpdateIsLocalizedAndNotAFullDocument() {
        given().queryParam("lang", "pt-BR").header("HX-Request", "true")
            .get("/buckets").then().statusCode(200)
            .body(containsString("Emulador indisponível"), not(containsString("<!doctype")));
    }
    @Test void processHealthDoesNotDependOnEmulator() {
        given().get("/q/health/live").then().statusCode(200);
    }
}
