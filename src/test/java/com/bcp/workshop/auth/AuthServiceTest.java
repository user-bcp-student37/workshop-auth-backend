package com.bcp.workshop.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthServiceTest {
    @Test
    void passesAuthenticatedUserToTokenIssuerAndMapsItsResult() {
        StubUserRepository repository = new StubUserRepository(
                new UserRecord(1, "student", "<b>A&B</b>", "STUDENT")
        );
        StubTokenIssuer tokenIssuer = new StubTokenIssuer();
        AuthService service = service(repository, tokenIssuer);

        LoginResponse response = service.login(request("student", "password"));

        assertEquals(1, repository.calls);
        assertEquals(1, tokenIssuer.calls);
        assertEquals("student", tokenIssuer.username);
        assertEquals("STUDENT", tokenIssuer.role);
        assertEquals("test-token", response.token());
        assertEquals(3600, response.expiresIn());
        assertEquals("student", response.user().username());
        assertEquals("&lt;b&gt;A&amp;B&lt;/b&gt;", response.user().displayName());
        assertEquals("STUDENT", response.user().role());
        assertEquals("authenticated-with-legacy-flow", response.debug());
    }

    @Test
    void rejectsInvalidCredentialsWithoutIssuingToken() {
        StubUserRepository repository = new StubUserRepository(null);
        StubTokenIssuer tokenIssuer = new StubTokenIssuer();
        AuthService service = service(repository, tokenIssuer);

        InvalidCredentialsException error = assertThrows(
                InvalidCredentialsException.class,
                () -> service.login(request("unknown", "password"))
        );

        assertEquals("unknown", error.username());
        assertEquals(1, repository.calls);
        assertEquals(0, tokenIssuer.calls);
    }

    @Test
    void rejectsInvalidRequestBeforeAccessingCollaborators() {
        StubUserRepository repository = new StubUserRepository(null);
        StubTokenIssuer tokenIssuer = new StubTokenIssuer();
        AuthService service = service(repository, tokenIssuer);

        LoginValidationException error = assertThrows(
                LoginValidationException.class,
                () -> service.login(request(" ", "password"))
        );

        assertEquals("Username is required", error.getMessage());
        assertEquals(0, repository.calls);
        assertEquals(0, tokenIssuer.calls);
    }

    private static AuthService service(StubUserRepository repository, StubTokenIssuer tokenIssuer) {
        return new AuthService(
                new LoginRequestValidator(),
                repository,
                tokenIssuer,
                new LoginResponseMapper()
        );
    }

    private static LoginRequest request(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.username = username;
        request.password = password;
        return request;
    }

    private static final class StubUserRepository implements UserRepository {
        private final UserRecord user;
        private int calls;

        private StubUserRepository(UserRecord user) {
            this.user = user;
        }

        @Override
        public UserRecord findByCredentials(String username, String password) {
            calls++;
            return user;
        }
    }

    private static final class StubTokenIssuer implements TokenIssuer {
        private int calls;
        private String username;
        private String role;

        @Override
        public IssuedToken issue(String username, String role) {
            calls++;
            this.username = username;
            this.role = role;
            return new IssuedToken("test-token", 3600);
        }
    }
}