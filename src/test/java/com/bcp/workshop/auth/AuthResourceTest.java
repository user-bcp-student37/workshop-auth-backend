package com.bcp.workshop.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(value = JwtTestResource.class, restrictToAnnotatedClass = true)
class AuthResourceTest {
    @Inject
    LegacyDatabase database;

    @Test
    void logsInWithWorkshopUser() throws Exception {
        Response response = given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .body("user.username", is("student"))
                .body("user.displayName", is("Workshop Student"))
                .body("user.role", is("STUDENT"))
                .body("token", notNullValue())
                .body("expiresIn", is(3600))
                .body("debug", is("authenticated-with-legacy-flow"))
                .extract().response();

        assertSignedToken(response.jsonPath().getString("token"), "student", "STUDENT");
    }

    @Test
    void rejectsInvalidCredentials() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"wrong-password\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(401)
                .body("message", is("Invalid user student or password"));
    }

    @Test
    void returnsGenericInternalErrorWhenCredentialLookupFails() throws Exception {
        database.initialize();
        String backupTable = "legacy_users_h08_backup_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = DriverManager.getConnection(databaseUrl());
             var statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE legacy_users RENAME TO " + backupTable);
        }

        try {
            given()
                    .contentType("application/json")
                    .body("{\"username\":\"student\",\"password\":\"Workshop123!\"}")
                    .when().post("/auth/login")
                    .then()
                    .statusCode(500)
                    .body("message", is("Internal server error"))
                    .body("exception", is("INTERNAL_SERVER_ERROR"));
        } finally {
            try (Connection connection = DriverManager.getConnection(databaseUrl());
                 var statement = connection.createStatement()) {
                statement.executeUpdate("ALTER TABLE " + backupTable + " RENAME TO legacy_users");
            }
        }
    }

    @Test
    void rejectsUnknownUsername() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"unknown-user\",\"password\":\"not-a-real-password\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(401)
                .body("message", is("Invalid user unknown-user or password"));
    }

    @Test
    void rejectsMissingRequestBody() {
        given()
                .contentType("application/json")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Request is null"));
    }

    @Test
    void rejectsMissingUsername() {
        given()
                .contentType("application/json")
                .body("{}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Username is required"));
    }

    @Test
    void rejectsBlankUsername() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"  \",\"password\":\"unused\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Username is required"));
    }

    @Test
    void rejectsMissingPassword() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Password is required"));
    }

    @Test
    void rejectsBlankPassword() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"  \"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Password is required"));
    }

    @Test
    void authenticatesPasswordWithoutTrimmingSignificantWhitespace() throws Exception {
        String username = "h08-whitespace-" + UUID.randomUUID();
        String password = "  pass phrase with spaces  ";

        try (Connection connection = DriverManager.getConnection(databaseUrl());
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO legacy_users(username,password,display_name,role) VALUES (?,?,?,?)")) {
            statement.setString(1, username);
            statement.setString(2, BcryptUtil.bcryptHash(password));
            statement.setString(3, "Whitespace Test");
            statement.setString(4, "TEST");
            statement.executeUpdate();
        }

        try {
            given()
                    .contentType("application/json")
                    .body("{\"username\":\"" + username + "\",\"password\":\"  pass phrase with spaces  \"}")
                    .when().post("/auth/login")
                    .then()
                    .statusCode(200)
                    .body("user.username", is(username));

            given()
                    .contentType("application/json")
                    .body("{\"username\":\"" + username + "\",\"password\":\"pass phrase with spaces\"}")
                    .when().post("/auth/login")
                    .then()
                    .statusCode(401);
        } finally {
            try (Connection connection = DriverManager.getConnection(databaseUrl());
                 PreparedStatement statement = connection.prepareStatement("DELETE FROM legacy_users WHERE username=?")) {
                statement.setString(1, username);
                statement.executeUpdate();
            }
        }
    }

    @Test
    void rejectsMalformedJsonWithEmptyFrameworkBody() {
        Response response = given()
                .contentType("application/json")
                .body("{malformed-json")
                .when().post("/auth/login");

        assertFrameworkError(response, 400);
    }

    @Test
    void rejectsUnsupportedRequestMediaTypeWithEmptyFrameworkBody() {
        Response response = given()
                .contentType("text/plain")
                .body("plain text")
                .when().post("/auth/login");

        assertFrameworkError(response, 415);
    }

    @Test
    void rejectsUnacceptableResponseMediaTypeWithEmptyFrameworkBody() {
        Response response = given()
                .accept("text/plain")
                .contentType("application/json")
                .body("{}")
                .when().post("/auth/login");

        assertFrameworkError(response, 406);
    }

    @Test
    void rejectsUnsupportedHttpMethodWithEmptyFrameworkBody() {
        Response response = given()
                .when().get("/auth/login");

        assertFrameworkError(response, 405);
    }

    @Test
    void acceptsOmittedClientAndUnknownJsonProperties() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"locale\":\"es\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .body("user.username", is("student"));
    }

    @Test
    void escapesHtmlInDisplayName() throws Exception {
        String username = "h06-html-" + UUID.randomUUID();
        String password = "test-only-h06-password";
        String displayName = "<b>A&B</b>";

        try (Connection connection = DriverManager.getConnection(databaseUrl())) {
            try (var schema = connection.createStatement()) {
                schema.executeUpdate("CREATE TABLE IF NOT EXISTS legacy_users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, password TEXT NOT NULL, display_name TEXT NOT NULL, role TEXT NOT NULL)");
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO legacy_users(username,password,display_name,role) VALUES (?,?,?,?)")) {
                statement.setString(1, username);
                statement.setString(2, BcryptUtil.bcryptHash(password));
                statement.setString(3, displayName);
                statement.setString(4, "TEST");
                statement.executeUpdate();
            }
        }

        try {
            given()
                    .contentType("application/json")
                    .body("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}")
                    .when().post("/auth/login")
                    .then()
                    .statusCode(200)
                    .body("user.displayName", is("&lt;b&gt;A&amp;B&lt;/b&gt;"));
        } finally {
            try (Connection connection = DriverManager.getConnection(databaseUrl());
                 PreparedStatement statement = connection.prepareStatement("DELETE FROM legacy_users WHERE username=?")) {
                statement.setString(1, username);
                statement.executeUpdate();
            }
        }
    }

    @Test
    void rejectsUsernameContainingQuote() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"stu'dent\",\"password\":\"not-a-real-password\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(401);
    }

    @Test
    void tokenHasVerifiableSignatureAndExplicitClaims() throws Exception {
        assertSignedToken(LegacyTokenUtil.create("test-user", "TEST"), "test-user", "TEST");
    }

    private static void assertSignedToken(String token, String username, String role) throws Exception {
        String[] sections = token.split("\\.");
        assertEquals(3, sections.length);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode header = objectMapper.readTree(Base64.getUrlDecoder().decode(sections[0]));
        JsonNode claims = objectMapper.readTree(Base64.getUrlDecoder().decode(sections[1]));
        assertEquals("RS256", header.path("alg").asText());
        assertEquals(username, claims.path("sub").asText());
        assertEquals(role, claims.path("role").asText());
        long issuedAt = claims.path("iat").asLong();
        long expiresAt = claims.path("exp").asLong();
        assertTrue(issuedAt <= Instant.now().getEpochSecond());
        assertEquals(3600, expiresAt - issuedAt);

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(JwtTestResource.verificationKey());
        signature.update((sections[0] + "." + sections[1]).getBytes(StandardCharsets.US_ASCII));
        assertTrue(signature.verify(Base64.getUrlDecoder().decode(sections[2])));

        byte[] alteredSignature = Base64.getUrlDecoder().decode(sections[2]);
        alteredSignature[0] ^= 1;
        signature.initVerify(JwtTestResource.verificationKey());
        signature.update((sections[0] + "." + sections[1]).getBytes(StandardCharsets.US_ASCII));
        org.junit.jupiter.api.Assertions.assertFalse(signature.verify(alteredSignature));
    }

    @Test
    void corsAllowsConfiguredLocalOrigin() {
        given()
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "POST")
                .when().options("/auth/login")
                .then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", is("http://localhost:4200"));
    }

    @Test
    void corsDoesNotAuthorizeUnconfiguredOrigin() {
        Response response = given()
                .header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "POST")
                .when().options("/auth/login");

        org.junit.jupiter.api.Assertions.assertNotEquals(
                "https://untrusted.example", response.getHeader("Access-Control-Allow-Origin"));
    }

    @Test
    void storesSeedPasswordAsBCryptHash() throws Exception {
        try (Connection connection = DriverManager.getConnection(databaseUrl());
             PreparedStatement statement = connection.prepareStatement("SELECT password FROM legacy_users WHERE username=?")) {
            statement.setString(1, "student");
            try (ResultSet result = statement.executeQuery()) {
                org.junit.jupiter.api.Assertions.assertTrue(result.next());
                String passwordHash = result.getString("password");
                org.junit.jupiter.api.Assertions.assertNotEquals("Workshop123!", passwordHash);
                org.junit.jupiter.api.Assertions.assertTrue(BcryptUtil.matches("Workshop123!", passwordHash));
            }
        }
    }

    @Test
    void bcryptDistinguishes64CharacterAsciiPasswords() {
        String firstPassword = "a".repeat(63) + "x";
        String differentPassword = "a".repeat(63) + "y";
        String passwordHash = BcryptUtil.bcryptHash(firstPassword);

        assertTrue(BcryptUtil.matches(firstPassword, passwordHash));
        org.junit.jupiter.api.Assertions.assertFalse(BcryptUtil.matches(differentPassword, passwordHash));
    }

    @Test
    void bcryptAliases64CharacterUnicodePasswordsAfter72Utf8Bytes() {
        String sharedPrefix = Character.toString(0x00e9).repeat(36);
        String firstPassword = sharedPrefix + "a".repeat(28);
        String differentPassword = sharedPrefix + "b".repeat(28);
        String passwordHash = BcryptUtil.bcryptHash(firstPassword);

        assertTrue(BcryptUtil.matches(firstPassword, passwordHash));
        assertTrue(BcryptUtil.matches(differentPassword, passwordHash));
    }

    @Test
    void migratesExistingPlainTextPasswordIdempotently() throws Exception {
        String username = "h02_migration_characterization";
        String password = "temporary-test-password";
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200);

        try (Connection connection = DriverManager.getConnection(databaseUrl());
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT OR REPLACE INTO legacy_users(username,password,display_name,role) VALUES (?,?,?,?)")) {
            statement.setString(1, username);
            statement.setString(2, password);
            statement.setString(3, "Migration Test");
            statement.setString(4, "TEST");
            statement.executeUpdate();
        }

        String migratedHash;
        try {
            database.initialize();
            try (Connection connection = DriverManager.getConnection(databaseUrl());
                 PreparedStatement statement = connection.prepareStatement("SELECT password FROM legacy_users WHERE username=?")) {
                statement.setString(1, username);
                try (ResultSet result = statement.executeQuery()) {
                    org.junit.jupiter.api.Assertions.assertTrue(result.next());
                    migratedHash = result.getString("password");
                    org.junit.jupiter.api.Assertions.assertNotEquals(password, migratedHash);
                    org.junit.jupiter.api.Assertions.assertTrue(BcryptUtil.matches(password, migratedHash));
                }
            }

            database.initialize();
            try (Connection connection = DriverManager.getConnection(databaseUrl());
                 PreparedStatement statement = connection.prepareStatement("SELECT password FROM legacy_users WHERE username=?")) {
                statement.setString(1, username);
                try (ResultSet result = statement.executeQuery()) {
                    org.junit.jupiter.api.Assertions.assertTrue(result.next());
                    org.junit.jupiter.api.Assertions.assertEquals(migratedHash, result.getString("password"));
                }
            }
        } finally {
            try (Connection connection = DriverManager.getConnection(databaseUrl());
                 PreparedStatement statement = connection.prepareStatement("DELETE FROM legacy_users WHERE username=?")) {
                statement.setString(1, username);
                statement.executeUpdate();
            }
        }
    }

    private static String databaseUrl() {
        return System.getenv().getOrDefault("AUTH_DB_URL", "jdbc:sqlite:./data/workshop.db");
    }

    private static void assertFrameworkError(Response response, int expectedStatus) {
        assertEquals(expectedStatus, response.statusCode());
        assertTrue(response.asString().isEmpty());
    }
}
