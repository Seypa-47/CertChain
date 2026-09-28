ALTER TABLE certificate
    ADD COLUMN pdf_artifact_status varchar(12),
    ADD COLUMN pdf_artifact_error varchar(200);

ALTER TABLE certificate
    ADD CONSTRAINT certificate_pdf_artifact_status_check
    CHECK (pdf_artifact_status IS NULL OR pdf_artifact_status IN ('READY', 'FAILED'));

ALTER TABLE certificate
    ADD CONSTRAINT certificate_pdf_artifact_key_check
    CHECK (pdf_storage_key IS NULL OR pdf_artifact_status = 'READY');
