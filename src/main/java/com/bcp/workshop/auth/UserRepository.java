package com.bcp.workshop.auth;

public interface UserRepository {
    UserRecord findByCredentials(String username, String password);
}