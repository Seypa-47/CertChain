package com.certchain;

import org.springframework.boot.SpringApplication;

/** Test-classpath entry point for a controlled local demonstration clock. */
public final class LocalDemoApplication {
    private LocalDemoApplication() {}

    public static void main(String[] args) {
        SpringApplication.run(new Class<?>[] {CertChainBackendApplication.class, LocalDemoClockConfig.class}, args);
    }
}
