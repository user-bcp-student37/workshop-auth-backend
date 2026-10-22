package com.bcp.workshop.auth;

import io.smallrye.jwt.build.Jwt;

import java.time.Instant;

public final class LegacyTokenUtil {
    public static final int TOKEN_EXPIRATION_SECONDS = 3600;

    private LegacyTokenUtil() {}

    public static String create(String username, String role) {
        Instant issuedAt = Instant.now();
        return Jwt.claims()
                .claim("sub", username)
                .claim("role", role)
                .claim("iat", issuedAt.getEpochSecond())
                .claim("exp", issuedAt.plusSeconds(TOKEN_EXPIRATION_SECONDS).getEpochSecond())
                .sign();
    }
}
