package com.certchain.common.security;

import com.certchain.auth.AuthSettings;
import com.certchain.auth.AuthenticatedPrincipal;
import com.certchain.auth.JwtTokenService;
import com.certchain.user.AppUser;
import com.certchain.user.AppUserRepository;
import com.certchain.user.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CookieAuthenticationFilter extends OncePerRequestFilter {
    private final AuthSettings settings;
    private final JwtTokenService tokens;
    private final AppUserRepository users;

    public CookieAuthenticationFilter(AuthSettings settings, JwtTokenService tokens, AppUserRepository users) {
        this.settings = settings;
        this.tokens = tokens;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            Arrays.stream(cookies).filter(cookie -> settings.cookieName().equals(cookie.getName()))
                .findFirst().ifPresent(cookie -> authenticate(cookie.getValue()));
        }
        chain.doFilter(request, response);
    }

    private void authenticate(String value) {
        try {
            Jwt jwt = tokens.verify(value);
            if (jwt.getIssuedAt() == null || jwt.getExpiresAt() == null) return;
            UUID id = UUID.fromString(jwt.getClaimAsString("user_id"));
            UUID organizationId = UUID.fromString(jwt.getClaimAsString("organization_id"));
            UserRole role = UserRole.valueOf(jwt.getClaimAsString("role"));
            if (!id.toString().equals(jwt.getSubject())) return;
            AppUser user = users.findByIdAndOrganizationId(id, organizationId).orElse(null);
            if (user == null || !user.getEnabled() || user.getRole() != role) return;
            AuthenticatedPrincipal principal = new AuthenticatedPrincipal(id, organizationId, role,
                user.getName(), user.getEmail());
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null,
                    java.util.List.of(new SimpleGrantedAuthority(role.name()))));
        } catch (JwtException | IllegalArgumentException invalid) {
            // An invalid cookie is unauthenticated. No token value is logged.
        }
    }
}
