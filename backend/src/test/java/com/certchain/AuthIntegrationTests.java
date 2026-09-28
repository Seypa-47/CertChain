package com.certchain;

import com.certchain.auth.AuthSettings;
import com.certchain.certificate.Certificate;
import com.certchain.certificate.CertificateIdGenerator;
import com.certchain.certificate.CertificateRepository;
import com.certchain.organization.Organization;
import com.certchain.organization.OrganizationRepository;
import com.certchain.user.AppUser;
import com.certchain.user.AppUserRepository;
import com.certchain.user.UserRole;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "app.auth.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
    "app.auth.cookie-secure=false",
    "app.cors.allowed-origins=http://localhost:3000"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class AuthIntegrationTests {
    @Container @ServiceConnection
    static final PostgreSQLContainer postgres =
        new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));

    @Autowired MockMvc mvc;
    @Autowired OrganizationRepository organizations;
    @Autowired AppUserRepository users;
    @Autowired CertificateRepository certificates;
    @Autowired CertificateIdGenerator ids;
    @Autowired PasswordEncoder passwords;
    @Autowired AuthSettings settings;

    AppUser admin;
    AppUser other;
    AppUser disabled;
    String password = "test-password-12345";

    @BeforeEach
    void seed() {
        Organization first = organizations.saveAndFlush(new Organization("First", UUID.randomUUID() + "@example.com"));
        Organization second = organizations.saveAndFlush(new Organization("Second", UUID.randomUUID() + "@example.com"));
        admin = users.saveAndFlush(new AppUser(first, "First Admin", UUID.randomUUID() + "@example.com",
            passwords.encode(password), UserRole.ORG_ADMIN));
        other = users.saveAndFlush(new AppUser(second, "Second Admin", UUID.randomUUID() + "@example.com",
            passwords.encode(password), UserRole.ORG_ADMIN));
        disabled = new AppUser(first, "Disabled", UUID.randomUUID() + "@example.com",
            passwords.encode(password), UserRole.ORG_ADMIN);
        disabled.setEnabled(false);
        disabled = users.saveAndFlush(disabled);
    }

    record Csrf(String token, Cookie cookie) {}
    private Csrf csrf() throws Exception {
        MockHttpServletResponse response = mvc.perform(get("/api/auth/csrf"))
            .andExpect(status().isOk()).andReturn().getResponse();
        String token = new tools.jackson.databind.ObjectMapper().readTree(response.getContentAsString())
            .get("token").asText();
        return new Csrf(token, response.getCookie("XSRF-TOKEN"));
    }

    private MockHttpServletResponse login(AppUser user, String enteredPassword) throws Exception {
        Csrf csrf = csrf();
        String json = "{\"email\":\"" + user.getEmail() + "\",\"password\":\"" + enteredPassword + "\"}";
        return mvc.perform(post("/api/auth/login").cookie(csrf.cookie()).header("X-XSRF-TOKEN", csrf.token())
            .contentType(MediaType.APPLICATION_JSON).content(json)).andReturn().getResponse();
    }

    private Cookie authCookie(AppUser user) throws Exception {
        MockHttpServletResponse response = login(user, password);
        assertEquals(200, response.getStatus());
        Cookie cookie = response.getCookie(settings.cookieName());
        assertNotNull(cookie);
        assertTrue(cookie.isHttpOnly());
        assertNull(new tools.jackson.databind.ObjectMapper().readTree(response.getContentAsString()).get("accessToken"));
        return cookie;
    }

    @Test void successfulLoginAndCurrentUser() throws Exception {
        Cookie cookie = authCookie(admin);
        mvc.perform(get("/api/auth/me").cookie(cookie))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(admin.getId().toString()))
            .andExpect(jsonPath("$.organizationId").value(admin.getOrganization().getId().toString()));
        mvc.perform(get("/api/organization").cookie(cookie))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(admin.getOrganization().getId().toString()));
    }

    @Test void badUnknownAndDisabledUsersAreIndistinguishable() throws Exception {
        assertEquals(401, login(admin, "bad-password").getStatus());
        assertEquals(401, login(disabled, password).getStatus());
        Csrf csrf = csrf();
        mvc.perform(post("/api/auth/login").cookie(csrf.cookie()).header("X-XSRF-TOKEN", csrf.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"unknown@example.com\",\"password\":\"password123456\"}"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }

    @Test void expiredAndTamperedTokensAreRejected() throws Exception {
        Cookie valid = authCookie(admin);
        String value = valid.getValue();
        String tampered = value.substring(0, value.length() - 2) + "xx";
        mvc.perform(get("/api/auth/me").cookie(new Cookie(settings.cookieName(), tampered)))
            .andExpect(status().isUnauthorized());
        Instant old = Instant.now().minusSeconds(3600);
        String expired = NimbusJwtEncoder.withSecretKey(new SecretKeySpec(
            Base64.getDecoder().decode("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="), "HmacSHA256"))
            .algorithm(MacAlgorithm.HS256).build()
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().issuer(settings.issuer()).subject(admin.getId().toString())
                    .issuedAt(old.minusSeconds(900)).expiresAt(old)
                    .claim("user_id", admin.getId().toString())
                    .claim("organization_id", admin.getOrganization().getId().toString())
                    .claim("role", "ORG_ADMIN").build())).getTokenValue();
        mvc.perform(get("/api/auth/me").cookie(new Cookie(settings.cookieName(), expired)))
            .andExpect(status().isUnauthorized());
    }

    @Test void csrfAndUnauthorizedAccessAreRejected() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        mvc.perform(post("/api/auth/logout").cookie(authCookie(admin)))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("CSRF_INVALID"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"a@example.com\",\"password\":\"password123\"}"))
            .andExpect(status().isForbidden());
    }

    @Test void roleAndCorsRulesApply() throws Exception {
        mvc.perform(get("/api/organization").with(user("viewer").authorities(() -> "VIEWER")))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
        mvc.perform(options("/api/auth/login").header("Origin", "http://localhost:3000")
            .header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
        mvc.perform(options("/api/auth/login").header("Origin", "https://evil.example")
            .header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isForbidden());
    }

    @Test void logoutClearsCookie() throws Exception {
        Cookie auth = authCookie(admin);
        Csrf csrf = csrf();
        mvc.perform(post("/api/auth/logout").cookie(auth, csrf.cookie())
            .header("X-XSRF-TOKEN", csrf.token()))
            .andExpect(status().isNoContent())
            .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
    }

    @Test void certificateLookupCannotCrossTenant() throws Exception {
        Certificate certificate = certificates.saveAndFlush(new Certificate(ids.nextId(), admin.getOrganization(),
            "Recipient", "recipient@example.com", "Course", LocalDate.now()));
        mvc.perform(get("/api/certificates/{id}", certificate.getId()).cookie(authCookie(admin)))
            .andExpect(status().isOk());
        mvc.perform(get("/api/certificates/{id}", certificate.getId()).cookie(authCookie(other)))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("CERTIFICATE_NOT_FOUND"));
    }

    @Test void issuanceEndpointRequiresAuthenticationCsrfAndTenantOwnership() throws Exception {
        Certificate certificate = certificates.saveAndFlush(new Certificate(ids.nextId(), admin.getOrganization(),
            "Recipient", "recipient@example.com", "Course", LocalDate.now()));
        Csrf token = csrf();
        mvc.perform(post("/api/certificates/{id}/issue", certificate.getId())
            .cookie(token.cookie()).header("X-XSRF-TOKEN", token.token()))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/certificates/{id}/issue", certificate.getId())
            .cookie(authCookie(admin)))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/certificates/{id}/issue", certificate.getId())
            .cookie(authCookie(other), token.cookie()).header("X-XSRF-TOKEN", token.token()))
            .andExpect(status().isNotFound());
        mvc.perform(post("/api/certificates/{id}/issue", certificate.getId())
            .cookie(authCookie(admin), token.cookie()).header("X-XSRF-TOKEN", token.token()))
            .andExpect(status().isServiceUnavailable());
    }

    @Test void revokeEndpointRequiresAuthenticationCsrfAndTenantOwnership() throws Exception {
        Certificate certificate = certificates.saveAndFlush(new Certificate(ids.nextId(), admin.getOrganization(),
            "Recipient", "recipient@example.com", "Course", LocalDate.now()));
        Csrf token = csrf();
        String body = "{\"reason\":\"Private correction\"}";
        mvc.perform(post("/api/certificates/{id}/revoke", certificate.getId())
            .cookie(token.cookie()).header("X-XSRF-TOKEN", token.token())
            .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/certificates/{id}/revoke", certificate.getId())
            .cookie(authCookie(admin)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/certificates/{id}/revoke", certificate.getId())
            .with(user("viewer").authorities(() -> "VIEWER"))
            .cookie(token.cookie()).header("X-XSRF-TOKEN", token.token())
            .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/certificates/{id}/revoke", certificate.getId())
            .cookie(authCookie(other), token.cookie()).header("X-XSRF-TOKEN", token.token())
            .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isNotFound());
        mvc.perform(post("/api/certificates/{id}/revoke", certificate.getId())
            .cookie(authCookie(admin), token.cookie()).header("X-XSRF-TOKEN", token.token())
            .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("CERTIFICATE_NOT_ISSUED"));
    }
}
