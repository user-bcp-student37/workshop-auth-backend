package com.bcp.workshop.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AuthService {
    private final LoginRequestValidator validator;
    private final UserRepository userRepository;
    private final TokenIssuer tokenIssuer;
    private final LoginResponseMapper responseMapper;

    @Inject
    public AuthService(
            LoginRequestValidator validator,
            UserRepository userRepository,
            TokenIssuer tokenIssuer,
            LoginResponseMapper responseMapper) {
        this.validator = validator;
        this.userRepository = userRepository;
        this.tokenIssuer = tokenIssuer;
        this.responseMapper = responseMapper;
    }

    public LoginResponse login(LoginRequest request) {
        String validationError = validator.validationError(request);
        if (validationError != null) {
            throw new LoginValidationException(validationError);
        }

        UserRecord user = userRepository.findByCredentials(request.username, request.password);
        if (user == null) {
            throw new InvalidCredentialsException(request.username);
        }

        TokenIssuer.IssuedToken token = tokenIssuer.issue(user.username(), user.role());
        return responseMapper.map(user, token);
    }
}