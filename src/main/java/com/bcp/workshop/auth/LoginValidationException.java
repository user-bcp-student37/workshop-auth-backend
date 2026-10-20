package com.bcp.workshop.auth;

public class LoginValidationException extends RuntimeException {
    public LoginValidationException(String message) {
        super(message);
    }
}