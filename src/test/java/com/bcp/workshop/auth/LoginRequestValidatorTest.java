package com.bcp.workshop.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LoginRequestValidatorTest {
    private final LoginRequestValidator validator = new LoginRequestValidator();

    @Test
    void rejectsNullRequest() {
        assertEquals("Request is null", validator.validationError(null));
    }

    @Test
    void rejectsMissingOrBlankUsername() {
        LoginRequest missing = new LoginRequest();
        missing.password = "password";
        assertEquals("Username is required", validator.validationError(missing));

        LoginRequest blank = new LoginRequest();
        blank.username = "  ";
        blank.password = "password";
        assertEquals("Username is required", validator.validationError(blank));
    }

    @Test
    void rejectsMissingOrBlankPassword() {
        LoginRequest missing = new LoginRequest();
        missing.username = "student";
        assertEquals("Password is required", validator.validationError(missing));

        LoginRequest blank = new LoginRequest();
        blank.username = "student";
        blank.password = "  ";
        assertEquals("Password is required", validator.validationError(blank));
    }

    @Test
    void acceptsNonblankCredentials() {
        LoginRequest request = new LoginRequest();
        request.username = " student ";
        request.password = " password ";
        assertNull(validator.validationError(request));
    }
}