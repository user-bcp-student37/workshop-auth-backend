package com.bcp.workshop.auth;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.text.StringEscapeUtils;

@ApplicationScoped
public class LoginResponseMapper {
    private static final String DEBUG_MESSAGE = "authenticated-with-legacy-flow";

    public LoginResponse map(UserRecord user, TokenIssuer.IssuedToken token) {
        LoginResponse.User responseUser = new LoginResponse.User(
                user.username(),
                StringEscapeUtils.escapeHtml4(user.displayName()),
                user.role()
        );
        return new LoginResponse(token.value(), token.expiresIn(), responseUser, DEBUG_MESSAGE);
    }
}