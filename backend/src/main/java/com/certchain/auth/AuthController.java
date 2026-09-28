package com.certchain.auth;

import com.certchain.auth.dto.LoginRequest;
import com.certchain.auth.dto.UserResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;
    private final AuthSettings settings;

    public AuthController(AuthService service, AuthSettings settings) {
        this.service = service;
        this.settings = settings;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getToken());
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result = service.login(request.email(), request.password());
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, authCookie(result.token(), settings.ttl()))
            .body(UserResponse.from(result.principal()));
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return UserResponse.from(principal);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, authCookie("", Duration.ZERO)).build();
    }

    private String authCookie(String value, Duration age) {
        ResponseCookie.ResponseCookieBuilder cookie = ResponseCookie.from(settings.cookieName(), value)
            .httpOnly(true).secure(settings.secure()).sameSite(settings.sameSite())
            .path("/").maxAge(age);
        if (!settings.domain().isBlank()) cookie.domain(settings.domain());
        return cookie.build().toString();
    }

    public record CsrfResponse(String token) {}
}
