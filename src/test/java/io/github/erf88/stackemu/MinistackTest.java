package io.github.erf88.stackemu;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import jakarta.inject.Inject;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import software.amazon.awssdk.services.s3.S3Client;

@QuarkusTest
@EnabledIfEnvironmentVariable(named = "STACKEMU_INTEGRATION", matches = "true")
class MinistackTest {
    @Inject S3Client s3;
    @Test void actualBucketAppearsInPageAndFragment() {
        String bucket = "stackemu-spike-" + java.util.UUID.randomUUID();
        s3.createBucket(b -> b.bucket(bucket));
        try {
            given().get("/").then().statusCode(200).body(containsString(bucket));
            given().get("/buckets").then().statusCode(200).body(containsString(bucket));
        } finally {
            // Delete only the empty test bucket created by this test.
            s3.deleteBucket(b -> b.bucket(bucket));
        }
    }
}
