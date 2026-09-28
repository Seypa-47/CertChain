package com.certchain.common.security;

import com.certchain.auth.AuthSettings;
import com.certchain.common.exception.SecurityErrorWriter;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") String configured) {
        List<String> origins = Arrays.stream(configured.split(",")).map(String::trim)
            .filter(origin -> !origin.isBlank()).toList();
        if (origins.isEmpty() || origins.stream().anyMatch(origin -> origin.contains("*"))) {
            throw new IllegalArgumentException("CORS_ALLOWED_ORIGINS must contain explicit origins");
        }
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(origins);
        cors.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
        cors.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthSettings settings,
                                            CookieAuthenticationFilter authentication,
                                            SecurityErrorWriter errors) throws Exception {
        CookieCsrfTokenRepository csrfCookies = new CookieCsrfTokenRepository();
        csrfCookies.setCookieCustomizer(cookie -> {
            cookie.secure(settings.secure()).sameSite(settings.sameSite()).path("/");
            if (!settings.domain().isBlank()) cookie.domain(settings.domain());
        });
        return http
            .csrf(csrf -> csrf.csrfTokenRepository(csrfCookies)
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout.disable())
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, exception) ->
                    errors.write(request, response, 401, "UNAUTHORIZED", "Authentication required"))
                .accessDeniedHandler((request, response, exception) ->
                    errors.write(request, response, 403,
                        exception instanceof CsrfException ? "CSRF_INVALID" : "ACCESS_DENIED",
                        exception instanceof CsrfException ? "CSRF token missing or invalid" : "Access denied")))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/api/auth/csrf", "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                .requestMatchers("/api/dashboard", "/api/organization", "/api/organization/**", "/api/certificates/**")
                    .hasAuthority("ORG_ADMIN")
                .anyRequest().authenticated())
            .addFilterBefore(authentication, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}

