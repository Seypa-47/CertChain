# Certificate Workflows

## Issuance

```mermaid
sequenceDiagram
    actor Admin
    participant UI as Next.js
    participant API as Spring Boot
    participant DB as PostgreSQL
    participant Chain as CertificateRegistry
    participant Store as File storage
    participant Mail as SMTP

    Admin->>UI: Confirm issue draft
    UI->>API: POST /certificates/{id}/issue
    API->>DB: Lock draft, set ISSUING, persist hash + transaction journal
    API->>Chain: issueCertificate(key, hash, expiresAt)
    Chain-->>API: transaction hash
    API->>DB: Mark transaction SUBMITTED
    Chain-->>API: confirmed receipt + CertificateIssued event
    API->>DB: Mark transaction CONFIRMED and certificate ISSUED
    API-->>UI: Issued certificate and blockchain metadata
```

Failure behavior:

- Validation or authorization failure stops before any chain call.
- A reverted or failed receipt sets the transaction to `FAILED` and certificate to `ISSUE_FAILED`; it is not public.
- A timeout remains `SUBMITTED`/`ISSUING` until reconciliation determines the receipt outcome.
- An RPC submission error with no returned hash remains `CREATED`/`ISSUING`: the transaction may have reached the chain. Reconciliation searches by deterministic key and event before any new submission is considered.
- PDF and email delivery are later phases; their failures must never undo immutable issuance.

## Public verification

```mermaid
sequenceDiagram
    actor Verifier
    participant UI as Next.js
    participant API as Spring Boot
    participant DB as PostgreSQL
    participant Chain as CertificateRegistry

    Verifier->>UI: Enter ID or open QR URL
    UI->>API: GET /public/certificates/{certificateId}
    API->>DB: Load issued certificate and organization
    API->>API: Rebuild canonical data and SHA-256 hash
    API->>Chain: get/verify by deterministic key
    Chain-->>API: immutable proof and revocation state
    API->>API: Compare hashes and derive REVOKED > EXPIRED > VALID
    API-->>UI: Public fields, status, proof result, explorer metadata
    UI-->>Verifier: Accessible status card and details
```

The QR contains only `https://<frontend>/verify/<certificateId>`. An unavailable RPC produces a clear verification-unavailable state, not a false valid result.

## Revocation

```mermaid
sequenceDiagram
    actor Admin
    participant UI as Next.js
    participant API as Spring Boot
    participant DB as PostgreSQL
    participant Chain as CertificateRegistry

    Admin->>UI: Enter reason and confirm
    UI->>API: POST /certificates/{id}/revoke
    API->>DB: Authorize owner, lock issued certificate, create REVOKE journal
    API->>Chain: revokeCertificate(key)
    Chain-->>API: transaction hash
    API->>DB: Mark transaction SUBMITTED
    Chain-->>API: confirmed receipt + CertificateRevoked event
    API->>DB: Mark CONFIRMED and set revokedAt
    API-->>UI: Status REVOKED
```

The reason is private and persisted before submission. The on-chain call carries only the certificate key. A missing receipt leaves the journal pending; a validated revert records failure without `revokedAt`. After a crash, reconciliation searches the known hash or indexed revocation event. Public verification derives `REVOKED > EXPIRED > VALID` on each read and never exposes the reason.

