package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.QuarkusTestProfile;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@TestProfile(AuthSigningFailureTest.MissingSigningKeyProfile.class)
class AuthSigningFailureTest {
    @Test
    void returnsDocumentedInternalServerErrorWhenSigningKeyIsMissing() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(500)
                .body("message", is("Internal server error"))
                .body("exception", is("INTERNAL_SERVER_ERROR"));
    }

    public static class MissingSigningKeyProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "smallrye.jwt.sign.key.location",
                    "target/nonexistent-signing-key-for-test.pem"
            );
        }
    }
}
