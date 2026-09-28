-- One journal per certificate and delivery purpose. Attempts are counted on that row.
CREATE UNIQUE INDEX email_delivery_certificate_type_uq
    ON email_delivery (certificate_id, delivery_type);
CREATE INDEX email_delivery_due_idx ON email_delivery (updated_at)
    WHERE status IN ('PENDING', 'FAILED');
