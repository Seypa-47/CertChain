package com.certchain.auth;

import com.certchain.user.AppUser;
import com.certchain.user.AppUserRepository;
import java.util.UUID;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final JwtTokenService tokens;
    private final String dummyHash;

    public AuthService(AppUserRepository users, PasswordEncoder encoder, JwtTokenService tokens) {
        this.users = users;
        this.encoder = encoder;
        this.tokens = tokens;
        this.dummyHash = encoder.encode(UUID.randomUUID().toString());
    }

    @Transactional(readOnly = true)
    public LoginResult login(String email, String password) {
        AppUser user = users.findByEmailIgnoreCase(email).orElse(null);
        boolean matches = encoder.matches(password, user == null ? dummyHash : user.getPasswordHash());
        if (user == null || !user.getEnabled() || !matches) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return new LoginResult(tokens.issue(user), new AuthenticatedPrincipal(
            user.getId(), user.getOrganization().getId(), user.getRole(), user.getName(), user.getEmail()));
    }

    public record LoginResult(String token, AuthenticatedPrincipal principal) {}
}
