package com.certchain.artifact;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VerificationUrlFactory {
    private final String base;

    public VerificationUrlFactory(@Value("${app.frontend-base-url}") String configuredBase) {
        URI parsed = URI.create(configuredBase);
        boolean localHttp = "http".equals(parsed.getScheme())
            && ("localhost".equals(parsed.getHost()) || "127.0.0.1".equals(parsed.getHost()));
        if ((!localHttp && !"https".equals(parsed.getScheme())) || parsed.getHost() == null
            || parsed.getUserInfo() != null || parsed.getRawQuery() != null || parsed.getRawFragment() != null
            || (parsed.getPath() != null && !parsed.getPath().isEmpty() && !"/".equals(parsed.getPath()))) {
            throw new IllegalArgumentException("Invalid frontend verification origin");
        }
        this.base = parsed.getScheme() + "://" + parsed.getRawAuthority();
    }

    public String forCertificate(String certificateId) {
        if (!certificateId.matches("CERT-[0-9]{4}-(?!000000)[0-9]{6}"))
            throw new IllegalArgumentException("Invalid public certificate ID");
        return base + "/verify/" + certificateId;
    }
}
