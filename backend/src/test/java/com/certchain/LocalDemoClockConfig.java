package com.certchain;

import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/** Test-classpath-only clock override for local expiry demonstrations. */
@Configuration
@Profile("local-demo")
public class LocalDemoClockConfig {
    @Bean
    @Primary
    Clock localDemoClock(@Value("${certchain.demo.clock-offset-days:0}") long offsetDays) {
        return Clock.offset(Clock.systemUTC(), Duration.ofDays(offsetDays));
    }
}
