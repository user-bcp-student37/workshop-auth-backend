package com.bcp.workshop.auth;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class JwtTokenIssuer implements TokenIssuer {
    @Override
    public IssuedToken issue(String username, String role) {
        return new IssuedToken(
                LegacyTokenUtil.create(username, role),
                LegacyTokenUtil.TOKEN_EXPIRATION_SECONDS
        );
    }
}