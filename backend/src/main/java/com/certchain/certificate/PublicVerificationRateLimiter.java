package com.certchain.certificate;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class PublicVerificationRateLimiter {
    private static final int LIMIT_PER_MINUTE = 120;
    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public PublicVerificationRateLimiter(Clock clock) { this.clock = clock; }

    public void check(String remoteAddress) {
        long minute = clock.instant().getEpochSecond() / 60;
        if (windows.size() > 10_000) windows.entrySet().removeIf(entry -> entry.getValue().minute < minute);
        Window window = windows.compute(remoteAddress, (key, old) -> old == null || old.minute != minute
            ? new Window(minute) : old);
        if (window.count.incrementAndGet() > LIMIT_PER_MINUTE) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
        }
    }

    private static final class Window {
        final long minute;
        final AtomicInteger count = new AtomicInteger();
        Window(long minute) { this.minute = minute; }
    }
}
