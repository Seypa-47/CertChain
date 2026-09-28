-- next_value is the next allocatable number. After issuing 999999 it is 1000000.
-- V1 remains unchanged so existing Flyway histories retain their checksum.
ALTER TABLE certificate_number_sequence
    DROP CONSTRAINT certificate_number_next_check;

-- V1 could leave 1000001 after an exhausted request; keep that year exhausted.
UPDATE certificate_number_sequence
SET next_value = LEAST(GREATEST(next_value, 2), 1000000)
WHERE next_value NOT BETWEEN 2 AND 1000000;

ALTER TABLE certificate_number_sequence
    ADD CONSTRAINT certificate_number_next_check
    CHECK (next_value BETWEEN 2 AND 1000000);
