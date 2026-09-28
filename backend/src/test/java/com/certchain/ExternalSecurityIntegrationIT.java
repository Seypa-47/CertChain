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
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Exercises the HTTP security boundary against a real PostgreSQL server without Docker. */
@SpringBootTest(properties = {
    "app.auth.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
    "app.auth.cookie-secure=false", "app.cors.allowed-origins=http://localhost:3000"
})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "CERTCHAIN_EXTERNAL_POSTGRES", matches = "true")
class ExternalSecurityIntegrationIT {
    @Autowired MockMvc mvc;
    @Autowired OrganizationRepository organizations;
    @Autowired AppUserRepository users;
    @Autowired CertificateRepository certificates;
    @Autowired CertificateIdGenerator ids;
    @Autowired PasswordEncoder passwords;
    @Autowired AuthSettings settings;

    private record Session(Cookie auth, Cookie csrfCookie, String csrfToken) {}

    private Session session(Organization organization) throws Exception {
        String password = "synthetic-test-password";
        AppUser user = users.saveAndFlush(new AppUser(organization, "Test Admin",
            UUID.randomUUID() + "@example.test", passwords.encode(password), UserRole.ORG_ADMIN));
        MockHttpServletResponse csrf = mvc.perform(get("/api/auth/csrf"))
            .andExpect(status().isOk()).andReturn().getResponse();
        String token = new tools.jackson.databind.ObjectMapper().readTree(csrf.getContentAsString())
            .get("token").asText();
        MockHttpServletResponse login = mvc.perform(post("/api/auth/login")
            .cookie(csrf.getCookie("XSRF-TOKEN")).header("X-XSRF-TOKEN", token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + user.getEmail() + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk()).andReturn().getResponse();
        Cookie auth = login.getCookie(settings.cookieName());
        assertNotNull(auth);
        assertTrue(auth.isHttpOnly());
        return new Session(auth, csrf.getCookie("XSRF-TOKEN"), token);
    }

    @Test
    void everyProtectedCertificateRouteRejectsAnotherTenant() throws Exception {
        Organization owner = organizations.saveAndFlush(new Organization("Owner", UUID.randomUUID() + "@example.test"));
        Organization outsider = organizations.saveAndFlush(new Organization("Outsider", UUID.randomUUID() + "@example.test"));
        Certificate certificate = certificates.saveAndFlush(new Certificate(ids.nextId(), owner,
            "Synthetic Recipient", "recipient@example.test", "Workshop", LocalDate.now()));
        Session foreign = session(outsider);
        UUID id = certificate.getId();

        for (String path : new String[] { "/api/certificates/{id}", "/api/certificates/{id}/issuance",
            "/api/certificates/{id}/revocation", "/api/certificates/{id}/pdf",
            "/api/certificates/{id}/pdf/status", "/api/certificates/{id}/email" }) {
            mvc.perform(get(path, id).cookie(foreign.auth())).andExpect(status().isNotFound());
        }
        mvc.perform(get("/api/certificates").cookie(foreign.auth()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(patch("/api/certificates/{id}", id).cookie(foreign.auth(), foreign.csrfCookie())
            .header("X-XSRF-TOKEN", foreign.csrfToken()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"recipientName\":\"Changed\",\"recipientEmail\":\"recipient@example.test\","
                + "\"programName\":\"Workshop\",\"issueDate\":\"" + LocalDate.now() + "\"}"))
            .andExpect(status().isNotFound());
        for (String path : new String[] { "/api/certificates/{id}/issue",
            "/api/certificates/{id}/reconcile", "/api/certificates/{id}/pdf/retry",
            "/api/certificates/{id}/email/resend", "/api/certificates/{id}/revocation/reconcile" }) {
            mvc.perform(post(path, id).cookie(foreign.auth(), foreign.csrfCookie())
                .header("X-XSRF-TOKEN", foreign.csrfToken())).andExpect(status().isNotFound());
        }
        mvc.perform(post("/api/certificates/{id}/revoke", id)
            .cookie(foreign.auth(), foreign.csrfCookie()).header("X-XSRF-TOKEN", foreign.csrfToken())
            .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Synthetic correction\"}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void authenticationCsrfAndCorsAreEnforced() throws Exception {
        Organization owner = organizations.saveAndFlush(new Organization("Owner", UUID.randomUUID() + "@example.test"));
        Session session = session(owner);
        mvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/dashboard").cookie(session.auth())).andExpect(status().isOk());
        mvc.perform(post("/api/certificates").cookie(session.auth())
            .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());
        mvc.perform(options("/api/auth/login").header("Origin", "http://localhost:3000")
            .header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
        mvc.perform(options("/api/auth/login").header("Origin", "https://evil.example")
            .header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isForbidden());
    }
}
