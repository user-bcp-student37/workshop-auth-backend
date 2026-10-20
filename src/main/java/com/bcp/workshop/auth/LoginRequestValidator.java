package com.bcp.workshop.auth;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LoginRequestValidator {
    public String validationError(LoginRequest request) {
        if (request == null) {
            return "Request is null";
        }
        if (request.username == null || request.username.trim().isEmpty()) {
            return "Username is required";
        }
        if (request.password == null || request.password.trim().isEmpty()) {
            return "Password is required";
        }
        return null;
    }
}