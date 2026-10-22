package com.bcp.workshop.auth;

public interface TokenIssuer {
    IssuedToken issue(String username, String role);

    record IssuedToken(String value, int expiresIn) {}
}