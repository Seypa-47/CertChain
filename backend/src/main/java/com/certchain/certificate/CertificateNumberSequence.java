package com.certchain.certificate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "certificate_number_sequence")
public class CertificateNumberSequence {
    @Id
    @Column(name = "sequence_year")
    private Integer sequenceYear;
    @Column(name = "next_value", nullable = false)
    private long nextValue;
    @Version
    @Column(nullable = false)
    private long version;

    protected CertificateNumberSequence() {}
    public CertificateNumberSequence(int sequenceYear, long nextValue) {
        if (sequenceYear < 1 || sequenceYear > 9999 || nextValue < 2 || nextValue > 1000000) {
            throw new IllegalArgumentException("Certificate sequence values are out of range");
        }
        this.sequenceYear = sequenceYear;
        this.nextValue = nextValue;
    }
    public Integer getSequenceYear() { return sequenceYear; }
    public long getNextValue() { return nextValue; }
    public long getVersion() { return version; }
}
