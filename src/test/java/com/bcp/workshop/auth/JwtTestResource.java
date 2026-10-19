package com.bcp.workshop.auth;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Map;

public class JwtTestResource implements QuarkusTestResourceLifecycleManager {
    private static volatile PublicKey verificationKey;
    private Path privateKeyFile;

    public static PublicKey verificationKey() {
        return verificationKey;
    }

    @Override
    public Map<String, String> start() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            verificationKey = keyPair.getPublic();

            Files.createDirectories(Path.of("target"));
            privateKeyFile = Files.createTempFile(Path.of("target"), "jwt-test-", ".pem");
            String encodedKey = Base64.getMimeEncoder(64, new byte[]{'\n'})
                    .encodeToString(keyPair.getPrivate().getEncoded());
            String pem = "-----BEGIN PRIVATE KEY-----\n" + encodedKey + "\n-----END PRIVATE KEY-----\n";
            Files.writeString(privateKeyFile, pem, StandardCharsets.US_ASCII);

            return Map.of("smallrye.jwt.sign.key.location", privateKeyFile.toAbsolutePath().toString());
        } catch (Exception error) {
            throw new IllegalStateException("Unable to create temporary JWT signing key for tests", error);
        }
    }

    @Override
    public void stop() {
        if (privateKeyFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(privateKeyFile);
        } catch (IOException error) {
            throw new IllegalStateException("Unable to delete temporary JWT signing key", error);
        } finally {
            verificationKey = null;
        }
    }
}
