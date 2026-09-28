package com.certchain.certificate;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class CertificateIdGenerator {
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public CertificateIdGenerator(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    public String nextId() {
        int year = LocalDate.now(clock).getYear();
        Long number;
        try {
            number = jdbc.queryForObject("""
            INSERT INTO certificate_number_sequence (sequence_year, next_value, version)
            VALUES (?, 2, 0)
            ON CONFLICT (sequence_year) DO UPDATE
            SET next_value = certificate_number_sequence.next_value + 1,
                version = certificate_number_sequence.version + 1
            WHERE certificate_number_sequence.next_value < 1000000
            RETURNING next_value - 1
            """, Long.class, year);
        } catch (EmptyResultDataAccessException exhausted) {
            throw new IllegalStateException("Certificate number range exhausted for " + year, exhausted);
        }
        if (number == null || number < 1 || number > 999999) {
            throw new IllegalStateException("Certificate number range exhausted for " + year);
        }
        return String.format(Locale.ROOT, "CERT-%04d-%06d", year, number);
    }
}
